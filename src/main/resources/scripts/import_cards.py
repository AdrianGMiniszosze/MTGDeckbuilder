#!/usr/bin/env python3
"""
MTG Card Import Script
Reads Scryfall bulk data (one JSON card per line) and upserts it into PostgreSQL.

The stopgap loader, not the production importer: it targets the post-V3 schema
(scryfall_id, oracle_id, copies_limit, is_basic_land, card_legality) and is meant
to be run by hand against a migrated database. See docs/DATA_IMPORT.md.

- One row per printing, identified by the Scryfall printing id (`scryfall_id`).
  Running the script again refreshes the row instead of duplicating it.
- The synthetic rows of a multi-face card carry no Scryfall id, so they are
  keyed by (card_name, set_id, collector_number).
- Derives cards.copies_limit from rule text ("any number / up to N cards named").
- Maps Scryfall legalities onto the formats seeded by Flyway.
- Extracts keywords into card_keywords (card_tag is reserved for semantic tags).
- Child tables use ON CONFLICT DO NOTHING, so a re-import is idempotent.
- Prices and rulings are not imported yet.
"""

import json
import os
import re
import sys
from typing import Dict, Iterator, List

import psycopg2
from psycopg2.extras import execute_batch

# Windows consoles default to a legacy code page; the progress output uses
# emoji, so do not crash when stdout is cp1252.
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, 'reconfigure'):
        _stream.reconfigure(encoding='utf-8', errors='replace')

# Database connection parameters
# Credentials are read from environment variables — never hardcode them here
DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': int(os.environ.get('DB_PORT', 5433)),
    'database': os.environ.get('DB_NAME', 'mtg_db'),
    'user': os.environ.get('DB_USERNAME', 'user'),
    'password': os.environ.get('DB_PASSWORD', '')
}

# Color mapping for MTG color codes
COLOR_MAP = {
    'W': 'W',  # White
    'U': 'U',  # Blue
    'B': 'B',  # Black
    'R': 'R',  # Red
    'G': 'G'   # Green
}

# Scryfall legality keys -> formats.format_name seeded by Flyway.
# Keys without a seeded format are ignored on purpose: the API reports those
# cards as LEGALITY_UNKNOWN instead of failing the import.
LEGALITY_FORMAT_MAP = {
    'standard': 'Standard',
    'modern': 'Modern',
    'legacy': 'Legacy',
    'vintage': 'Vintage',
    'pauper': 'Pauper',
    'commander': 'Commander',
}

# cards.copies_limit: NULL defers to the format, 0 means no limit, N is an
# exact cap. Scryfall exposes no such field, so it is derived from rule text.
# The rule is always worded as deck construction ("A deck can have ..."); a
# bare "up to N cards named X" also appears in tutoring effects (Howling Wolf)
# and must not cap the deck.
_ANY_NUMBER_RE = re.compile(r'a deck can have any number of cards named', re.IGNORECASE)
_UP_TO_NUMBER_RE = re.compile(
    r'a deck can have up to (one|two|three|four|five|six|seven|eight|nine|ten|\d+) '
    r'cards named',
    re.IGNORECASE,
)
_NUMBER_WORDS = {
    'one': 1, 'two': 2, 'three': 3, 'four': 4, 'five': 5,
    'six': 6, 'seven': 7, 'eight': 8, 'nine': 9, 'ten': 10,
}

# Cards that are not playable printings and are skipped before any insert.
SKIPPED_LAYOUTS = ('token', 'emblem', 'art_series')


def detect_copies_limit(oracle_text: str):
    """Map 'any number / up to N cards named X' rule text to copies_limit."""
    if not oracle_text:
        return None
    if _ANY_NUMBER_RE.search(oracle_text):
        return 0
    match = _UP_TO_NUMBER_RE.search(oracle_text)
    if match:
        token = match.group(1).lower()
        return int(token) if token.isdigit() else _NUMBER_WORDS[token]
    return None


def is_basic_land(supertype: str, card_type: str) -> bool:
    """Basic lands are exempt from the copies limit."""
    supertypes = (supertype or '').lower().split()
    types = (card_type or '').lower().split()
    return 'basic' in supertypes and 'land' in types


def iter_cards(json_file_path: str) -> Iterator[dict]:
    """Yield cards from a Scryfall JSONL file without loading it into memory.

    A full bulk file is around 600 MB; reading it whole needed several GB of
    RAM. One JSON object per line is the format Scryfall publishes.
    """
    with open(json_file_path, 'r', encoding='utf-8-sig') as handle:
        for line in handle:
            line = line.strip().rstrip(',')
            if not line or line in ('[', ']'):
                continue
            try:
                card = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(card, dict):
                yield card


class CardImporter:
    # Columns written by this script. Kept in one place so the insert, the
    # update and the schema check cannot drift apart again.
    CARD_COLUMNS = (
        'card_name', 'scryfall_id', 'oracle_id', 'mana_cost', 'cmc', 'color_identity',
        'type_line', 'card_type', 'card_supertype', 'rarity', 'card_text', 'flavor_text',
        'power', 'toughness', 'copies_limit', 'is_basic_land', 'image_url', 'foil',
        'game_changer', 'language', 'set_id', 'collector_number', 'promo', 'variation',
    )

    def __init__(self):
        self.conn = None
        self.cursor = None
        self.set_cache: Dict[str, int] = {}  # set_name -> set_id
        self.format_cache: Dict[str, int] = {}  # lowercase format name -> format_id

        self.cards_processed = 0
        self.cards_inserted = 0
        self.cards_updated = 0
        self.cards_skipped = 0
        self.rows_failed = 0

        # Detailed skip tracking
        self.skip_reasons = {
            'tokens_emblems': 0,
            'missing_name': 0,
            'db_errors': 0,
            'general_errors': 0,
            'validation_errors': 0,
        }
        self.skipped_cards = []  # Store details of skipped cards

        # Multi-face card tracking
        self.multi_face_stats = {
            'adventure': 0,
            'split': 0,
            'transform': 0,
            'modal_dfc': 0,
            'flip': 0,
            'multi_face': 0,
            'total_faces_created': 0,
            'total_combined_created': 0,
        }

    # ------------------------------------------------------------------
    # Connection and caches
    # ------------------------------------------------------------------

    def connect(self):
        """Establish database connection"""
        try:
            self.conn = psycopg2.connect(**DB_CONFIG)
            self.cursor = self.conn.cursor()
            print(f"✓ Connected to database: {DB_CONFIG['database']}")
        except Exception as e:
            print(f"✗ Failed to connect to database: {e}")
            sys.exit(1)

    def load_set_cache(self):
        """Load existing sets into cache"""
        self.cursor.execute("SELECT id, name FROM sets")
        for set_id, name in self.cursor.fetchall():
            self.set_cache[name] = set_id
        print(f"✓ Loaded {len(self.set_cache)} existing sets into cache")

    def load_format_cache(self):
        """Load formats seeded by Flyway (V2 and V3)."""
        self.cursor.execute("SELECT id, format_name FROM formats")
        for format_id, name in self.cursor.fetchall():
            self.format_cache[name.lower()] = format_id
        print(f"✓ Loaded {len(self.format_cache)} formats into cache")

    def get_or_create_set(self, set_name: str, set_code: str = None) -> int:
        """Get set ID from cache or create new set.

        `sets.id` is ours; Scryfall's `set_id` is a UUID and is never used
        here. The Scryfall set code goes into `sets.code`.
        """
        cached = self.set_cache.get(set_name)
        if cached:
            return cached

        self.cursor.execute("SELECT id FROM sets WHERE name = %s", (set_name,))
        row = self.cursor.fetchone()
        if row:
            set_id = row[0]
            if set_code:
                self.cursor.execute(
                    "UPDATE sets SET code = COALESCE(code, %s), updated_at = now() WHERE id = %s",
                    (set_code, set_id),
                )
        else:
            self.cursor.execute(
                "INSERT INTO sets (name, code) VALUES (%s, %s) RETURNING id",
                (set_name, set_code),
            )
            set_id = self.cursor.fetchone()[0]

        self.set_cache[set_name] = set_id
        return set_id

    def insert_card_legalities(self, card_id: int, legalities: dict) -> int:
        """Store per-format legality for one printing.

        Legality is not required to save a deck: a card with no rows is
        reported as LEGALITY_UNKNOWN by the API. So this is best effort and
        never aborts the card insert.
        """
        if not legalities:
            return 0

        rows = []
        for key, status in legalities.items():
            format_name = LEGALITY_FORMAT_MAP.get(key.lower())
            if not format_name:
                continue
            format_id = self.format_cache.get(format_name.lower())
            if not format_id:
                print(f"    ! Format '{format_name}' is not seeded; skipping legality")
                continue
            if status not in ('legal', 'not_legal', 'restricted', 'banned'):
                continue
            rows.append((card_id, format_id, status))

        if not rows:
            return 0

        execute_batch(self.cursor, """
            INSERT INTO card_legality (card_id, format_id, legality_status)
            VALUES (%s, %s, %s)
            ON CONFLICT (card_id, format_id) DO UPDATE
                SET legality_status = EXCLUDED.legality_status
        """, rows)
        return len(rows)

    # ------------------------------------------------------------------
    # Parsing helpers
    # ------------------------------------------------------------------

    def parse_type_line(self, type_line: str) -> tuple:
        """
        Parse type line into supertype, type, and subtype components.

        Args:
            type_line: Full type line (e.g., "Legendary Creature — Goblin Wizard")

        Returns:
            Tuple of (supertype, type, subtype) strings

        Examples:
            "Creature — Plant Wall" -> ("", "Creature", "Plant Wall")
            "Legendary Creature — Goblin Wizard" -> ("Legendary", "Creature", "Goblin Wizard")
            "Basic Land — Mountain" -> ("Basic", "Land", "Mountain")
            "Artifact Creature — Golem" -> ("", "Artifact Creature", "Golem")
            "Instant" -> ("", "Instant", "")
        """
        card_supertype = ''
        card_type = ''
        card_subtype = ''

        if not type_line:
            return (card_supertype, card_type, card_subtype)

        # Split on — to separate main types from subtypes
        parts = type_line.split('—')
        left_part = parts[0].strip() if len(parts) > 0 else ''
        card_subtype = parts[1].strip() if len(parts) > 1 else ''

        # Extract supertype and type from left part
        # Supertypes come BEFORE the main type
        left_tokens = left_part.split()
        supertypes = ['Basic', 'Elite', 'Legendary', 'Ongoing', 'Snow', 'World']
        types = ['Land', 'Creature', 'Artifact', 'Enchantment', 'Planeswalker',
                 'Instant', 'Sorcery', 'Tribal', 'Battle', 'Kindred']

        supertype_parts = []
        type_parts = []

        for token in left_tokens:
            if token in supertypes:
                supertype_parts.append(token)
            elif token in types:
                type_parts.append(token)

        card_supertype = ' '.join(supertype_parts)
        card_type = ' '.join(type_parts)

        return (card_supertype, card_type, card_subtype)

    def parse_subtypes(self, subtype_string: str) -> List[str]:
        """
        Parse subtypes from a subtype string.
        Handles the special case of "Time Lord" which is a two-word creature type.

        Examples:
            "Goblin Wizard" -> ["Goblin", "Wizard"]
            "Plant Wall" -> ["Plant", "Wall"]
            "Time Lord" -> ["Time Lord"]
            "Human Time Lord" -> ["Human", "Time Lord"]
            "Forest" -> ["Forest"]
        """
        if not subtype_string:
            return []

        # Special handling for "Time Lord" - the only two-word creature type
        if "Time Lord" in subtype_string:
            # Replace "Time Lord" with a placeholder, split, then restore
            temp = subtype_string.replace("Time Lord", "TIME_LORD_PLACEHOLDER")
            subtypes = temp.split()
            # Restore "Time Lord"
            return [s.replace("TIME_LORD_PLACEHOLDER", "Time Lord") for s in subtypes]

        # For all other cases, split by space
        return subtype_string.split()

    # ------------------------------------------------------------------
    # Card writes
    # ------------------------------------------------------------------

    def _find_card_id(self, scryfall_id, card_name: str, set_id: int, collector_number: str):
        """Return the existing row for this printing, if any."""
        if scryfall_id:
            self.cursor.execute(
                "SELECT id FROM cards WHERE scryfall_id = %s", (scryfall_id,))
        else:
            # Synthetic faces have no printing identity of their own.
            self.cursor.execute(
                "SELECT id FROM cards WHERE scryfall_id IS NULL AND card_name = %s "
                "AND set_id IS NOT DISTINCT FROM %s AND collector_number = %s "
                "ORDER BY id LIMIT 1",
                (card_name, set_id, collector_number),
            )
        row = self.cursor.fetchone()
        return row[0] if row else None

    def _insert_child_rows(self, card_id: int, card_data: dict, card_type: str,
                           card_supertype: str, card_subtype: str):
        """Write colors, types and keywords for one card.

        Every child table is keyed by (card_id, value), so a re-import must
        ignore the duplicate instead of aborting the transaction.
        """
        for color_code in card_data.get('colors') or []:
            if color_code in COLOR_MAP:
                self.cursor.execute(
                    "INSERT INTO card_colors (card_id, color) VALUES (%s, %s) "
                    "ON CONFLICT (card_id, color) DO NOTHING",
                    (card_id, COLOR_MAP[color_code]),
                )

        for color_code in card_data.get('color_identity') or []:
            if color_code in COLOR_MAP:
                self.cursor.execute(
                    "INSERT INTO card_color_identity (card_id, color) VALUES (%s, %s) "
                    "ON CONFLICT (card_id, color) DO NOTHING",
                    (card_id, COLOR_MAP[color_code]),
                )

        if card_type:
            for type_name in card_type.split():
                if type_name.strip():
                    self.cursor.execute(
                        "INSERT INTO card_types (card_id, type) VALUES (%s, %s) "
                        "ON CONFLICT (card_id, type) DO NOTHING",
                        (card_id, type_name.strip()),
                    )

        if card_supertype:
            for supertype_name in card_supertype.split():
                if supertype_name.strip():
                    self.cursor.execute(
                        "INSERT INTO card_supertypes (card_id, supertype) VALUES (%s, %s) "
                        "ON CONFLICT (card_id, supertype) DO NOTHING",
                        (card_id, supertype_name.strip()),
                    )

        # Keywords live in card_keywords only. card_tag is reserved for
        # semantic tags produced by other sources, so the importer does not
        # create tags with a meaningless weight.
        for keyword in card_data.get('keywords') or []:
            if keyword and keyword.strip():
                self.cursor.execute(
                    "INSERT INTO card_keywords (card_id, keyword) VALUES (%s, %s) "
                    "ON CONFLICT (card_id, keyword) DO NOTHING",
                    (card_id, keyword.strip()),
                )

        if card_subtype:
            for subtype in self.parse_subtypes(card_subtype):
                if subtype.strip():
                    self.cursor.execute(
                        "INSERT INTO card_subtypes (card_id, subtype) VALUES (%s, %s) "
                        "ON CONFLICT (card_id, subtype) DO NOTHING",
                        (card_id, subtype.strip()),
                    )

    def write_card(self, card_data: dict) -> tuple:
        """Upsert one printing and its child rows.

        Returns (card_id, created). Identity is the Scryfall printing id; the
        synthetic faces of a multi-face card fall back to
        (card_name, set_id, collector_number) so a re-import does not duplicate
        them.
        """
        card_name = card_data['name']
        scryfall_id = card_data.get('scryfall_id') or card_data.get('id')
        set_name = card_data.get('set_name', 'Unknown')
        set_code = card_data.get('set_code') or card_data.get('set')
        set_id = self.get_or_create_set(set_name, set_code)

        mana_cost = card_data.get('mana_cost') or ''
        try:
            cmc = int(card_data.get('cmc') or 0)
        except (TypeError, ValueError):
            cmc = 0

        type_line = card_data.get('type_line') or 'Unknown'
        card_supertype, card_type, card_subtype = self.parse_type_line(type_line)
        if not card_type:
            card_type = 'Unknown'

        oracle_text = card_data.get('oracle_text') or ''
        rarity = card_data.get('rarity') or 'common'
        image_url = (card_data.get('image_uris') or {}).get('normal') or 'https://example.com/missing.jpg'
        language = card_data.get('lang') or card_data.get('language') or 'en'
        collector_number = card_data.get('collector_number') or 'unknown'
        color_identity_list = card_data.get('color_identity') or []
        color_identity_str = ','.join(color_identity_list) if color_identity_list else None

        values = (
            card_name,
            scryfall_id,
            card_data.get('oracle_id'),
            mana_cost or None,
            cmc,
            color_identity_str,
            type_line,
            card_type,
            card_supertype or None,
            rarity,
            oracle_text,
            card_data.get('flavor_text') or None,
            card_data.get('power'),
            card_data.get('toughness'),
            detect_copies_limit(oracle_text),
            is_basic_land(card_supertype, card_type),
            image_url,
            bool(card_data.get('foil', False)),
            bool(card_data.get('game_changer', False)),
            language,
            set_id,
            collector_number,
            bool(card_data.get('promo', False)),
            bool(card_data.get('variation', False)),
        )

        existing_id = self._find_card_id(scryfall_id, card_name, set_id, collector_number)
        if existing_id:
            assignments = ', '.join(f'{column} = %s' for column in self.CARD_COLUMNS)
            self.cursor.execute(
                f"UPDATE cards SET {assignments}, updated_at = now() "
                "WHERE id = %s RETURNING id",
                values + (existing_id,),
            )
            card_id = self.cursor.fetchone()[0]
            created = False
        else:
            columns = ', '.join(self.CARD_COLUMNS)
            placeholders = ', '.join(['%s'] * len(self.CARD_COLUMNS))
            updates = ', '.join(
                f'{column} = EXCLUDED.{column}'
                for column in self.CARD_COLUMNS if column != 'scryfall_id'
            )
            self.cursor.execute(
                f"INSERT INTO cards ({columns}) VALUES ({placeholders}) "
                f"ON CONFLICT (scryfall_id) DO UPDATE SET {updates}, updated_at = now() "
                "RETURNING id",
                values,
            )
            card_id = self.cursor.fetchone()[0]
            created = True

        self.insert_card_legalities(card_id, card_data.get('legalities'))
        self._insert_child_rows(card_id, card_data, card_type, card_supertype, card_subtype)
        return card_id, created

    def _rollback_to(self, savepoint: str):
        """Discard the current card without discarding the rest of the batch."""
        try:
            self.cursor.execute(f"ROLLBACK TO SAVEPOINT {savepoint}")
            self.cursor.execute(f"RELEASE SAVEPOINT {savepoint}")
        except psycopg2.Error:
            # The connection itself is broken; the batch is lost anyway.
            self.conn.rollback()

    def insert_card_and_get_id(self, card_data: dict):
        """Write one synthetic face row and return its id (None on failure)."""
        if card_data.get('layout') in SKIPPED_LAYOUTS:
            return None

        self.cursor.execute("SAVEPOINT face_insert")
        try:
            card_id, created = self.write_card(card_data)
            self.cursor.execute("RELEASE SAVEPOINT face_insert")
            if created:
                self.cards_inserted += 1
            else:
                self.cards_updated += 1
            return card_id
        except Exception as e:
            self._rollback_to('face_insert')
            self.rows_failed += 1
            print(f"✗ ERROR inserting face '{card_data.get('name', 'Unknown')}': {e}")
            return None

    # ------------------------------------------------------------------
    # Multi-face cards
    # ------------------------------------------------------------------

    def process_multi_face_card(self, card_data: dict) -> bool:
        """
        Process cards with card_faces as separate cards.
        Handles Adventure, Split, Transform, Modal Double-Faced, and other multi-face cards.
        Creates multiple cards: one for each face + one combined reference.

        Returns True if this was a multi-face card, False otherwise.
        """
        card_faces = card_data.get('card_faces')
        if not card_faces or len(card_faces) < 2:
            return False

        # Check if this is a multi-face card (has // in name)
        card_name = card_data.get('name', '')
        layout = card_data.get('layout', '')

        if '//' not in card_name:
            return False

        # Determine the card type based on layout and content
        card_type = self.determine_multi_face_type(card_data, card_faces)

        print(f"🎭 PROCESSING {card_type.upper()} CARD WITH FACES: '{card_name}'")
        print(f"    Layout: {layout}, Faces: {len(card_faces)}")

        # Extract common card data
        set_name = card_data.get('set_name', 'Unknown')
        set_code = card_data.get('set')
        rarity = card_data.get('rarity', 'common')
        language = card_data.get('lang', 'en')
        collector_number = card_data.get('collector_number', 'unknown')
        promo = card_data.get('promo', False)
        variation = card_data.get('variation', False)

        # Process each face as a separate card
        face_card_ids = []
        face_suffixes = ['a', 'b', 'c', 'd']  # Support up to 4 faces

        for i, face in enumerate(card_faces):
            face_card_data = {
                # Basic info from face
                'name': face.get('name'),
                'mana_cost': face.get('mana_cost', ''),
                'type_line': face.get('type_line', ''),
                'oracle_text': face.get('oracle_text', ''),
                'power': face.get('power'),
                'toughness': face.get('toughness'),

                # Printing identity: a face has no Scryfall id of its own
                'scryfall_id': None,
                'oracle_id': card_data.get('oracle_id'),
                'legalities': card_data.get('legalities', {}),

                # Common data from main card
                'set_name': set_name,
                'set_code': set_code,
                'rarity': rarity,
                'lang': language,
                'collector_number': f"{collector_number}{face_suffixes[i] if i < len(face_suffixes) else str(i)}",
                'promo': promo,
                'variation': variation,

                # Image and other data
                'image_uris': face.get('image_uris', card_data.get('image_uris', {})),
                'colors': face.get('colors', []),
                'color_identity': face.get('color_identity', card_data.get('color_identity', [])),
                'cmc': face.get('cmc', card_data.get('cmc', 0)),
                'keywords': face.get('keywords', []),
                'layout': f'{card_type}_face'  # adventure_face, split_face, transform_face, etc.
            }

            face_name = face_card_data.get('name', f'Face {i+1}')
            face_type = face_card_data.get('type_line', 'Unknown')
            print(f"    ➤ Face {i+1}: '{face_name}' - {face_type}")

            # Insert the face as a separate card and get its ID
            face_card_id = self.insert_card_and_get_id(face_card_data)
            if face_card_id:
                face_card_ids.append(face_card_id)

        # Create the combined reference card
        combined_oracle_text = self.build_combined_oracle_text(card_faces, card_type)

        combined_card_data = {
            'name': card_name,  # Full combined name with //
            'scryfall_id': card_data.get('id'),
            'oracle_id': card_data.get('oracle_id'),
            'legalities': card_data.get('legalities', {}),
            'mana_cost': card_faces[0].get('mana_cost', ''),  # Use first face mana cost
            'type_line': card_data.get('type_line', ''),  # Full combined type line
            'oracle_text': combined_oracle_text,
            'power': card_faces[0].get('power'),  # Use first face power/toughness
            'toughness': card_faces[0].get('toughness'),

            # Common data
            'set_name': set_name,
            'set_code': set_code,
            'rarity': rarity,
            'lang': language,
            'collector_number': collector_number,  # Original collector number
            'promo': promo,
            'variation': variation,

            # Combined data
            'image_uris': card_data.get('image_uris', {}),
            'colors': card_data.get('colors', []),
            'color_identity': card_data.get('color_identity', []),
            'cmc': card_data.get('cmc', 0),
            'keywords': card_data.get('keywords', []),
            'layout': f'{card_type}_combined'  # Mark as combined
        }

        print(f"    ➤ Combined: '{combined_card_data['name']}'")

        # Insert the combined reference card and get its ID
        combined_card_id = self.insert_card_and_get_id(combined_card_data)
        if combined_card_id:
            # Create relationships between cards
            self.create_multi_face_relationships(combined_card_id, face_card_ids, card_type)

        # Update statistics
        self.multi_face_stats[card_type] += 1
        self.multi_face_stats['total_faces_created'] += len(face_card_ids)
        if combined_card_id:
            self.multi_face_stats['total_combined_created'] += 1

        print(f"    ✅ {card_type.capitalize()} card processing complete: {len(face_card_ids)} faces + "
              f"{1 if combined_card_id else 0} combined")
        print()

        return True

    def create_multi_face_relationships(self, combined_card_id: int, face_card_ids: list, card_type: str):
        """Create relationships between multi-face cards (faces and the combined card)"""
        try:
            for i, face_card_id in enumerate(face_card_ids):
                relationship_type = f'{card_type}_face'  # adventure_face, split_face, transform_face, etc.
                face_order = i  # 0 for first face, 1 for second face, etc.

                # Link combined card to face
                self.cursor.execute("""
                    INSERT INTO card_relationships (
                        parent_card_id, related_card_id, relationship_type, face_order
                    ) VALUES (%s, %s, %s, %s)
                    ON CONFLICT (parent_card_id, related_card_id, relationship_type) DO NOTHING
                """, (combined_card_id, face_card_id, relationship_type, face_order))

                # Link face to combined card (reverse relationship)
                self.cursor.execute("""
                    INSERT INTO card_relationships (
                        parent_card_id, related_card_id, relationship_type, face_order
                    ) VALUES (%s, %s, %s, %s)
                    ON CONFLICT (parent_card_id, related_card_id, relationship_type) DO NOTHING
                """, (face_card_id, combined_card_id, f'{card_type}_combined', face_order))

            print(f"    ✅ Created {card_type} relationships for {len(face_card_ids)} faces with combined card")

        except Exception as e:
            print(f"    ✗ ERROR creating {card_type} relationships: {e}")

    def determine_multi_face_type(self, card_data: dict, card_faces: list) -> str:
        """
        Determine the type of multi-face card based on layout and content.
        """
        layout = card_data.get('layout', '').lower()

        # Check layout first (most reliable)
        if layout == 'adventure':
            return 'adventure'
        elif layout == 'split':
            return 'split'
        elif layout == 'transform' or layout == 'double_faced_token':
            return 'transform'
        elif layout == 'modal_dfc':
            return 'modal_dfc'
        elif layout == 'flip':
            return 'flip'

        # Fallback: analyze faces content
        for face in card_faces:
            type_line = face.get('type_line', '')
            if 'Adventure' in type_line:
                return 'adventure'

        # Check if both faces are instants/sorceries (likely split)
        face_types = [face.get('type_line', '').lower() for face in card_faces]
        if all(any(t in face_type for t in ['instant', 'sorcery']) for face_type in face_types):
            return 'split'

        # Default fallback
        return 'multi_face'

    def build_combined_oracle_text(self, card_faces: list, card_type: str) -> str:
        """
        Build combined oracle text based on card type.
        """
        if card_type == 'adventure':
            # Adventure format: [Main] ... [Adventure] ...
            main_text = card_faces[0].get('oracle_text', '') if len(card_faces) > 0 else ''
            adventure_text = card_faces[1].get('oracle_text', '') if len(card_faces) > 1 else ''
            return f"[Main] {main_text} [Adventure] {adventure_text}"

        elif card_type == 'split':
            # Split format: [Left] ... [Right] ...
            texts = []
            sides = ['Left', 'Right', 'Third', 'Fourth']
            for i, face in enumerate(card_faces):
                side_name = sides[i] if i < len(sides) else f'Side {i+1}'
                face_text = face.get('oracle_text', '')
                if face_text:
                    texts.append(f"[{side_name}] {face_text}")
            return ' '.join(texts)

        elif card_type == 'transform':
            # Transform format: [Front] ... [Back] ...
            front_text = card_faces[0].get('oracle_text', '') if len(card_faces) > 0 else ''
            back_text = card_faces[1].get('oracle_text', '') if len(card_faces) > 1 else ''
            return f"[Front] {front_text} [Back] {back_text}"

        else:
            # Generic format: [Face 1] ... [Face 2] ...
            texts = []
            for i, face in enumerate(card_faces):
                face_text = face.get('oracle_text', '')
                if face_text:
                    texts.append(f"[Face {i + 1}] {face_text}")
            return ' '.join(texts)

    # ------------------------------------------------------------------
    # Input card entry point
    # ------------------------------------------------------------------

    def insert_card(self, card_data: dict):
        """Insert or refresh a single card from the bulk file."""
        # Tokens first: a double-faced token also has card_faces and "//" in
        # its name, so the multi-face path must not see it.
        layout = card_data.get('layout')
        if layout in SKIPPED_LAYOUTS:
            card_name = card_data.get('name', 'Unknown')
            print(f"⊘ SKIPPED - Token/Emblem: '{card_name}' (layout: {layout})")
            self.skip_reasons['tokens_emblems'] += 1
            self.cards_skipped += 1
            self.skipped_cards.append({
                'name': card_name,
                'reason': 'token_emblem',
                'details': f'layout={layout}'
            })
            return False

        card_name = card_data.get('name')
        if not card_name:
            print(f"⊘ SKIPPED - Missing name: {card_data.get('id', 'Unknown ID')}")
            self.skip_reasons['missing_name'] += 1
            self.cards_skipped += 1
            self.skipped_cards.append({
                'name': 'NO_NAME',
                'reason': 'missing_name',
                'details': f'keys={list(card_data.keys())[:5]}'
            })
            return False

        if self.process_multi_face_card(card_data):
            return True

        # A card without both its written type and its printing metadata is
        # not worth a row: the API could never validate it.
        type_line = card_data.get('type_line') or ''
        _, parsed_type, _ = self.parse_type_line(type_line)
        critical_missing = []
        if not type_line:
            critical_missing.append('type_line')
        if not parsed_type:
            critical_missing.append('card_type')
        if not (card_data.get('image_uris') or {}).get('normal'):
            critical_missing.append('image_url')
        if not card_data.get('rarity'):
            critical_missing.append('rarity')

        if len(critical_missing) > 2:
            print(f"⊘ SKIPPED - Too many missing critical fields: '{card_name}' - {critical_missing}")
            self.skip_reasons['validation_errors'] += 1
            self.cards_skipped += 1
            self.skipped_cards.append({
                'name': card_name,
                'reason': 'validation_error',
                'details': f'missing_fields={critical_missing}'
            })
            return False

        self.cursor.execute("SAVEPOINT card_insert")
        try:
            _, created = self.write_card(card_data)
            self.cursor.execute("RELEASE SAVEPOINT card_insert")
            if created:
                self.cards_inserted += 1
            else:
                self.cards_updated += 1
            return True

        except psycopg2.Error as db_error:
            self._rollback_to('card_insert')
            print(f"⊘ SKIPPED - DB ERROR: '{card_name}'")
            print(f"    Error Code: {db_error.pgcode}")
            print(f"    Error Message: {db_error.pgerror}")
            print(f"    SQL State: {getattr(db_error, 'sqlstate', 'Unknown')}")
            print(f"    set_name: '{card_data.get('set_name', 'NULL')}'")

            self.skip_reasons['db_errors'] += 1
            self.cards_skipped += 1
            self.rows_failed += 1
            self.skipped_cards.append({
                'name': card_name,
                'reason': 'database_error',
                'details': f'pgcode={db_error.pgcode}, error={str(db_error)[:100]}'
            })
            return False

        except Exception as e:
            self._rollback_to('card_insert')
            print(f"⊘ SKIPPED - GENERAL ERROR: '{card_name}' - {type(e).__name__}: {str(e)}")

            self.skip_reasons['general_errors'] += 1
            self.cards_skipped += 1
            self.rows_failed += 1
            self.skipped_cards.append({
                'name': card_name,
                'reason': 'general_error',
                'details': f'{type(e).__name__}: {str(e)[:100]}'
            })
            return False

    # ------------------------------------------------------------------
    # Import driver
    # ------------------------------------------------------------------

    def import_cards(self, json_file_path: str, batch_size: int = 100):
        """Import cards from a Scryfall JSONL file."""
        print(f"\n📖 Reading JSON file: {json_file_path}")
        print(f"\n💾 Importing cards (batch size: {batch_size})...")

        try:
            processed = 0
            for card in iter_cards(json_file_path):
                processed += 1
                self.cards_processed += 1
                self.insert_card(card)

                # Commit in batches; a failing card rolls back only its
                # savepoint, so the rest of the batch survives.
                if processed % batch_size == 0:
                    self.conn.commit()
                    print(f"  Processed {processed} cards... "
                          f"(Inserted: {self.cards_inserted}, Updated: {self.cards_updated}, "
                          f"Skipped: {self.cards_skipped}, Failed rows: {self.rows_failed})")
        except Exception as e:
            print(f"✗ Failed to read JSON file: {e}")
            self.conn.rollback()
            return

        # Final commit
        self.conn.commit()

        # Print enhanced summary with multi-face statistics
        print(f"\n{'=' * 60}")
        print("📊 DETAILED IMPORT SUMMARY")
        print(f"{'=' * 60}")
        print(f"✓ Cards processed:    {self.cards_processed}")
        print(f"✓ Cards inserted:     {self.cards_inserted}")
        print(f"↻ Cards updated:      {self.cards_updated}")
        print(f"⊘ Cards skipped:      {self.cards_skipped}")
        print(f"✗ Failed rows:        {self.rows_failed}")

        # Multi-face card statistics
        total_multi_face = sum(
            count for key, count in self.multi_face_stats.items()
            if key not in ['total_faces_created', 'total_combined_created']
        )
        if total_multi_face > 0:
            print("")
            print("🎭 Multi-Face Card Statistics:")
            for card_type, count in self.multi_face_stats.items():
                if count > 0 and card_type not in ['total_faces_created', 'total_combined_created']:
                    print(f"  {card_type.capitalize():12s}: {count:4d} cards")
            print(f"  {'Total faces':12s}: {self.multi_face_stats['total_faces_created']:4d}")
            print(f"  {'Combined refs':12s}: {self.multi_face_stats['total_combined_created']:4d}")

        # Skip breakdown if any cards were skipped
        if self.cards_skipped > 0:
            print("")
            print("📋 Skip Breakdown:")
            for reason, count in self.skip_reasons.items():
                if count > 0:
                    reason_display = reason.replace('_', ' ').title()
                    print(f"  {reason_display:15s}: {count}")

        print(f"{'=' * 60}\n")

        # Export detailed skip log if any cards were skipped
        if self.cards_skipped > 0:
            self.export_skipped_cards_log()

    def diagnose_table_structure(self):
        """Check that the columns this script writes exist in `cards`."""
        try:
            print("\n🔍 DIAGNOSING TABLE STRUCTURE...")

            self.cursor.execute("""
                SELECT EXISTS (
                    SELECT FROM information_schema.tables
                    WHERE table_schema = 'public'
                    AND table_name = 'cards'
                );
            """)
            table_exists = self.cursor.fetchone()[0]
            print(f"✓ Cards table exists: {table_exists}")

            if not table_exists:
                print("✗ ERROR: Cards table does not exist!")
                return False

            self.cursor.execute("""
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                AND table_name = 'cards'
                ORDER BY ordinal_position;
            """)

            columns = self.cursor.fetchall()
            print(f"\n📋 CARDS TABLE STRUCTURE ({len(columns)} columns):")
            print("=" * 70)

            for i, (col_name, data_type, nullable, default) in enumerate(columns, 1):
                nullable_str = "NULL" if nullable == "YES" else "NOT NULL"
                default_str = f" DEFAULT {default}" if default else ""
                print(f"{i:2d}. {col_name:20s} {data_type:15s} {nullable_str:8s}{default_str}")

            print("=" * 70)

            actual_columns = [col[0] for col in columns]

            print("\n🔍 SCRIPT COMPATIBILITY ANALYSIS:")
            print("=" * 70)

            missing_columns = []
            print("Checking required columns for script:")
            for expected in self.CARD_COLUMNS:
                if expected in actual_columns:
                    print(f"  ✅ {expected:20s} - EXISTS")
                else:
                    print(f"  ❌ {expected:20s} - MISSING!")
                    missing_columns.append(expected)

            print("\nColumns in DB not used by script:")
            extra_columns = []
            for actual in actual_columns:
                if actual not in self.CARD_COLUMNS:
                    print(f"  ℹ️  {actual:20s} - Not used by script")
                    extra_columns.append(actual)

            print("=" * 70)

            if missing_columns:
                print("\n🚨 CRITICAL ERROR: Missing required columns!")
                print("   The following columns are required but don't exist:")
                for col in missing_columns:
                    print(f"     ❌ {col}")
                print("   ⛔ Import will FAIL if you proceed!")
                return False

            if extra_columns:
                print("\n💡 Additional columns in database:")
                for col in extra_columns:
                    print(f"     ℹ️  {col}")
                print("   📝 These might indicate schema differences or unused fields.")

            return True

        except Exception as e:
            print(f"✗ Error diagnosing table: {e}")
            return False

    def analyze_skips_in_json(self, json_file_path: str):
        """Analyze potential skip reasons and multi-face cards before importing."""
        print(f"\n🔍 PRE-IMPORT ANALYSIS: {json_file_path}")
        print("-" * 60)

        try:
            skip_analysis = {
                'tokens': 0,
                'missing_name': 0,
                'missing_type': 0,
                'missing_rarity': 0,
                'missing_image': 0,
                'multi_face_cards': 0,
                'problematic_layouts': []
            }

            layout_counts = {}
            multi_face_types = {}
            total_cards = 0

            for card in iter_cards(json_file_path):
                total_cards += 1
                layout = card.get('layout', 'unknown')
                layout_counts[layout] = layout_counts.get(layout, 0) + 1

                # Check for multi-face cards
                if card.get('card_faces') and '//' in card.get('name', ''):
                    skip_analysis['multi_face_cards'] += 1

                    # Determine multi-face type
                    multi_face_type = 'unknown'
                    if layout == 'adventure':
                        multi_face_type = 'adventure'
                    elif layout == 'split':
                        multi_face_type = 'split'
                    elif layout == 'transform':
                        multi_face_type = 'transform'
                    elif layout == 'modal_dfc':
                        multi_face_type = 'modal_dfc'
                    elif layout == 'flip':
                        multi_face_type = 'flip'

                    multi_face_types[multi_face_type] = multi_face_types.get(multi_face_type, 0) + 1

                if layout in SKIPPED_LAYOUTS:
                    skip_analysis['tokens'] += 1
                    if layout not in skip_analysis['problematic_layouts']:
                        skip_analysis['problematic_layouts'].append(layout)

                if not card.get('name'):
                    skip_analysis['missing_name'] += 1
                if not card.get('type_line'):
                    skip_analysis['missing_type'] += 1
                if not card.get('rarity'):
                    skip_analysis['missing_rarity'] += 1
                if not (card.get('image_uris') or {}).get('normal'):
                    skip_analysis['missing_image'] += 1

            print(f"📊 Analysis of {total_cards} total cards:")

            # Show multi-face cards info
            if skip_analysis['multi_face_cards'] > 0:
                print(f"🎭 Multi-face cards found: {skip_analysis['multi_face_cards']}")
                print("   Types breakdown:")
                for mf_type, count in sorted(multi_face_types.items(), key=lambda x: x[1], reverse=True):
                    print(f"     {mf_type:12s}: {count:4d} cards")
                print(f"   → These become {skip_analysis['multi_face_cards'] * 2} extra rows "
                      f"(faces + combined)")

            # Show predicted skips
            total_predicted_skips = 0
            print("\n📊 Predicted skips:")
            for reason, count in skip_analysis.items():
                if reason not in ['problematic_layouts', 'multi_face_cards'] and count > 0:
                    print(f"  {reason}: {count}")
                    total_predicted_skips += count

            print("\n📋 Layout distribution:")
            for layout, count in sorted(layout_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
                status = "❌ SKIP" if layout in SKIPPED_LAYOUTS else "✅ OK"
                multi_face_indicator = (
                    " 🎭 MULTI-FACE"
                    if layout in ['adventure', 'split', 'transform', 'modal_dfc', 'flip']
                    else ""
                )
                print(f"  {layout:15s}: {count:5d} cards {status}{multi_face_indicator}")

            # Calculate final statistics
            base_cards = total_cards - skip_analysis['multi_face_cards'] - total_predicted_skips
            total_expected_cards = (
                base_cards + skip_analysis['multi_face_cards'] * 3
            )

            print("\n🎯 Expected import results:")
            print(f"  📥 Input cards: {total_cards}")
            print(f"  ⊘ Skipped: {total_predicted_skips}")
            print(f"  🎭 Multi-face: {skip_analysis['multi_face_cards']} "
                  f"(become {skip_analysis['multi_face_cards'] * 3})")
            print(f"  ✅ Regular: {base_cards}")
            print(f"  📤 Total DB rows: {total_expected_cards}")
            if total_cards:
                print(f"  📊 Success rate: "
                      f"{((total_cards - total_predicted_skips) / total_cards * 100):.1f}%")
            print("-" * 60)

            return skip_analysis

        except Exception as e:
            print(f"✗ Error analyzing JSON: {e}")
            return None

    def export_skipped_cards_log(self, output_file: str = "skipped_cards.log"):
        """Export detailed skip information to a log file"""
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write("=" * 80 + "\n")
                f.write("MTG CARD IMPORT - DETAILED SKIP LOG\n")
                f.write("=" * 80 + "\n\n")

                f.write("Summary:\n")
                f.write(f"  Total cards skipped: {self.cards_skipped}\n")
                f.write(f"  Total cards inserted: {self.cards_inserted}\n")
                f.write(f"  Total cards updated: {self.cards_updated}\n")
                f.write(f"  Total failed rows: {self.rows_failed}\n\n")

                f.write("Skip reasons breakdown:\n")
                for reason, count in self.skip_reasons.items():
                    if count > 0:
                        f.write(f"  {reason}: {count}\n")
                f.write("\n")

                f.write("Multi-face cards processing:\n")
                for card_type, count in self.multi_face_stats.items():
                    if count > 0:
                        f.write(f"  {card_type}: {count}\n")
                f.write("\n")

                # Group by reason
                skip_groups = {}
                for skip in self.skipped_cards:
                    reason = skip['reason']
                    if reason not in skip_groups:
                        skip_groups[reason] = []
                    skip_groups[reason].append(skip)

                # Write detailed information for each reason
                for reason, skips in skip_groups.items():
                    f.write(f"\n{reason.upper()} ({len(skips)} cards):\n")
                    f.write("-" * 50 + "\n")

                    for i, skip in enumerate(skips, 1):
                        f.write(f"{i:3d}. {skip['name']}\n")
                        f.write(f"     Details: {skip['details']}\n")
                    f.write("\n")

            print(f"📝 Detailed skip log exported to: {output_file}")

        except Exception as e:
            print(f"✗ Error exporting skip log: {e}")

    def close(self):
        """Close database connection"""
        if self.cursor:
            self.cursor.close()
        if self.conn:
            self.conn.close()
        print("✓ Database connection closed")


def main():
    # Default JSON file path
    json_file = "default-cards-20251101090854.json"

    # Check if custom path provided
    if len(sys.argv) > 1:
        json_file = sys.argv[1]

    print(f"""
{'=' * 60}
🃏  MTG Card Importer
{'=' * 60}
Database: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}
JSON File: {json_file}
{'=' * 60}
    """)

    importer = CardImporter()

    try:
        importer.connect()

        # PRE-IMPORT ANALYSIS
        print("\n" + "=" * 60)
        print("🔍 PRE-IMPORT CARD ANALYSIS")
        print("=" * 60)
        importer.analyze_skips_in_json(json_file)

        # DIAGNOSE TABLE STRUCTURE
        print("\n" + "=" * 60)
        print("🔍 ANALYZING DATABASE STRUCTURE")
        print("=" * 60)

        if not importer.diagnose_table_structure():
            print("✗ Cannot proceed - table structure diagnosis failed")
            return

        print("\n" + "=" * 60)
        print("✅ SCHEMA COMPATIBLE: cards columns match the post-V3 schema")
        print("🔁 IDEMPOTENT: rows are upserted by scryfall_id")
        print("🎭 MULTI-FACE: Adventure, Split, Transform, Modal DFC cards supported")
        print("-" * 60)
        print("🚀 Proceeding with import...")

        # Load caches and start import
        importer.load_set_cache()
        importer.load_format_cache()
        importer.import_cards(json_file, batch_size=100)
    except KeyboardInterrupt:
        print("\n\n⚠ Import cancelled by user")
    except Exception as e:
        print(f"\n✗ Import failed: {e}")
    finally:
        importer.close()


if __name__ == "__main__":
    main()
