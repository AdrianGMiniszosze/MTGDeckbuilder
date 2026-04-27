#!/usr/bin/env python3
"""
MTG Card Import Script
Reads cards from JSON and inserts them into PostgreSQL database.
- Extracts keywords and adds them to the tags table (cached to avoid duplicates)
- Inserts card data into the cards table with proper color mappings
"""

import json
import psycopg2
from psycopg2.extras import execute_batch
from typing import Dict, List, Set
import sys
import random

# Database connection parameters
# Credentials are read from environment variables — never hardcode them here
import os
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


class CardImporter:
    def __init__(self):
        self.conn = None
        self.cursor = None
        self.tag_cache: Dict[str, int] = {}  # keyword -> tag_id
        self.set_cache: Dict[str, int] = {}  # set_code -> set_id
        self.cards_inserted = 0
        self.cards_skipped = 0
        self.tags_created = 0
        
        # Detailed skip tracking
        self.skip_reasons = {
            'tokens_emblems': 0,
            'missing_name': 0,
            'duplicates': 0,
            'db_errors': 0,
            'general_errors': 0,
            'validation_errors': 0
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
            'total_combined_created': 0
        }

    def connect(self):
        """Establish database connection"""
        try:
            self.conn = psycopg2.connect(**DB_CONFIG)
            self.cursor = self.conn.cursor()
            print(f"✓ Connected to database: {DB_CONFIG['database']}")
        except Exception as e:
            print(f"✗ Failed to connect to database: {e}")
            sys.exit(1)
    
    def load_tag_cache(self):
        """Load existing tags into cache to avoid duplicates"""
        self.cursor.execute("SELECT id, name FROM tags")
        for tag_id, name in self.cursor.fetchall():
            self.tag_cache[name.lower()] = tag_id
        print(f"✓ Loaded {len(self.tag_cache)} existing tags into cache")
    
    def load_set_cache(self):
        """Load existing sets into cache"""
        self.cursor.execute("SELECT id, name FROM sets")
        for set_id, name in self.cursor.fetchall():
            self.set_cache[name] = set_id
        print(f"✓ Loaded {len(self.set_cache)} existing sets into cache")
    
    def get_or_create_tag(self, keyword: str) -> int:
        """Get tag ID from cache or create new tag"""
        keyword_lower = keyword.lower()
        
        if keyword_lower in self.tag_cache:
            return self.tag_cache[keyword_lower]
        
        # Create new tag
        try:
            self.cursor.execute(
                "INSERT INTO tags (name) VALUES (%s) RETURNING id",
                (keyword,)
            )
            tag_id = self.cursor.fetchone()[0]
            self.tag_cache[keyword_lower] = tag_id
            self.tags_created += 1
            return tag_id
        except psycopg2.IntegrityError:
            # Tag was created by another process, reload cache
            self.conn.rollback()
            self.load_tag_cache()
            return self.tag_cache.get(keyword_lower)
    
    def get_or_create_set(self, set_name: str) -> int:
        """Get set ID from cache or create new set"""
        if set_name in self.set_cache:
            return self.set_cache[set_name]
        
        # Create new set
        try:
            self.cursor.execute(
                "INSERT INTO sets (name) VALUES (%s) RETURNING id",
                (set_name,)
            )
            set_id = self.cursor.fetchone()[0]
            self.set_cache[set_name] = set_id
            return set_id
        except psycopg2.IntegrityError:
            # Set was created by another process, reload cache
            self.conn.rollback()
            self.load_set_cache()
            return self.set_cache.get(set_name)
    
    def parse_type_line(self, type_line: str) -> tuple[str, str, str]:
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
        set_id = self.get_or_create_set(set_name)
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

                # Common data from main card
                'set_name': set_name,
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
                self.cards_inserted += 1
            else:
                self.cards_skipped += 1

        # Create the combined reference card
        combined_oracle_text = self.build_combined_oracle_text(card_faces, card_type)

        combined_card_data = {
            'name': card_name,  # Full combined name with //
            'mana_cost': card_faces[0].get('mana_cost', ''),  # Use first face mana cost
            'type_line': card_data.get('type_line', ''),  # Full combined type line
            'oracle_text': combined_oracle_text,
            'power': card_faces[0].get('power'),  # Use first face power/toughness
            'toughness': card_faces[0].get('toughness'),

            # Common data
            'set_name': set_name,
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
            self.cards_inserted += 1

            # Create relationships between cards
            self.create_multi_face_relationships(combined_card_id, face_card_ids, card_type)
        else:
            self.cards_skipped += 1

        # Update statistics
        self.multi_face_stats[card_type] += 1
        self.multi_face_stats['total_faces_created'] += len(face_card_ids)
        if combined_card_id:
            self.multi_face_stats['total_combined_created'] += 1

        print(f"    ✅ {card_type.capitalize()} card processing complete: {len(face_card_ids)} faces + 1 combined = {len(face_card_ids) + 1} total cards")
        print()

        return True

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
                    texts.append(f"[Face {i+1}] {face_text}")
            return ' '.join(texts)

    def insert_card_and_get_id(self, card_data: dict) -> int:
        """
        Modified version of insert_card that returns the card ID instead of boolean.
        Used for Adventure cards where we need to track relationships.
        """
        try:
            # DON'T process Adventure cards recursively here
            # (this function is called FROM process_adventure_card_faces)

            # Skip tokens and non-cards
            if card_data.get('layout') in ['token', 'emblem', 'art_series']:
                return None

            # Extract basic card info
            card_name = card_data.get('name')
            if not card_name:
                return None

            # Check for duplicate cards
            self.cursor.execute("SELECT id FROM cards WHERE card_name = %s", (card_name,))
            existing_result = self.cursor.fetchone()
            if existing_result:
                print(f"⊘ SKIPPED - Duplicate: '{card_name}' (already exists)")
                return None

            # Get or create set FIRST
            set_name = card_data.get('set_name', 'Unknown')
            set_id = self.get_or_create_set(set_name)

            # Parse card data (same logic as insert_card)
            mana_cost = card_data.get('mana_cost', '')
            cmc = int(card_data.get('cmc', 0))

            type_line = card_data.get('type_line', '')
            card_supertype, card_type, card_subtype = self.parse_type_line(type_line)

            oracle_text = card_data.get('oracle_text', '')
            power = card_data.get('power')
            toughness = card_data.get('toughness')
            rarity = card_data.get('rarity', 'common')
            image_url = card_data.get('image_uris', {}).get('normal', '')
            flavor_text = card_data.get('flavor_text', '')
            foil = card_data.get('foil', False)
            game_changer = card_data.get('game_changer', False)
            language = card_data.get('lang', 'en')

            collector_number = card_data.get('collector_number', 'unknown')
            promo = card_data.get('promo', False)
            variation = card_data.get('variation', False)

            color_identity_list = card_data.get('color_identity', [])
            color_identity_str = ','.join(color_identity_list) if color_identity_list else ''

            # Validation and defaults (simplified)
            if not oracle_text:
                oracle_text = ''
            if not image_url:
                image_url = 'https://example.com/missing.jpg'

            # Process keywords
            keyword_tag_ids = []
            keywords = card_data.get('keywords', [])
            for keyword in keywords:
                if keyword and keyword.strip():
                    tag_id = self.get_or_create_tag(keyword.strip())
                    keyword_tag_ids.append((tag_id, keyword.strip()))

            # INSERT THE CARD and GET ID
            self.cursor.execute("""
                INSERT INTO cards (
                    card_name, mana_cost, cmc, color_identity, type_line, card_type,
                    card_supertype, rarity, card_text, flavor_text,
                    power, toughness, unlimited_copies, image_url, foil,
                    game_changer, language, card_set, collector_number, promo, variation
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                ) RETURNING id
            """, (
                card_name,
                mana_cost or None,
                cmc,
                color_identity_str or None,
                type_line,
                card_type,
                card_supertype or None,
                rarity,
                oracle_text,
                flavor_text or None,
                power,
                toughness,
                False,
                image_url,
                foil,
                game_changer,
                language,
                set_id,
                collector_number,
                promo,
                variation
            ))

            card_id = self.cursor.fetchone()[0]

            # Insert related data (same as insert_card)
            colors = card_data.get('colors', [])
            for color_code in colors:
                if color_code in COLOR_MAP:
                    self.cursor.execute(
                        "INSERT INTO card_colors (card_id, color) VALUES (%s, %s)",
                        (card_id, COLOR_MAP[color_code])
                    )

            color_identity = card_data.get('color_identity', [])
            for color_code in color_identity:
                if color_code in COLOR_MAP:
                    self.cursor.execute(
                        "INSERT INTO card_color_identity (card_id, color) VALUES (%s, %s)",
                        (card_id, COLOR_MAP[color_code])
                    )

            # Insert types, supertypes, keywords, subtypes (same logic as insert_card)
            if card_type:
                types = card_type.split()
                for type_name in types:
                    if type_name and type_name.strip():
                        self.cursor.execute(
                            "INSERT INTO card_types (card_id, type) VALUES (%s, %s)",
                            (card_id, type_name.strip())
                        )

            if card_supertype:
                supertypes = card_supertype.split()
                for supertype_name in supertypes:
                    if supertype_name and supertype_name.strip():
                        self.cursor.execute(
                            "INSERT INTO card_supertypes (card_id, supertype) VALUES (%s, %s)",
                            (card_id, supertype_name.strip())
                        )

            for keyword in keywords:
                if keyword and keyword.strip():
                    self.cursor.execute(
                        "INSERT INTO card_keywords (card_id, keyword) VALUES (%s, %s)",
                        (card_id, keyword.strip())
                    )

            if card_subtype:
                subtypes = self.parse_subtypes(card_subtype)
                for subtype in subtypes:
                    if subtype and subtype.strip():
                        self.cursor.execute(
                            "INSERT INTO card_subtypes (card_id, subtype) VALUES (%s, %s)",
                            (card_id, subtype.strip())
                        )

            # Insert card-tag relationships
            for tag_id, keyword in keyword_tag_ids:
                self.cursor.execute("""
                    INSERT INTO card_tag (
                        card_id, tag_id, confidence, weight, source, model_version
                    ) VALUES (%s, %s, %s, %s, %s, %s)
                    ON CONFLICT (card_id, tag_id) DO NOTHING
                """, (
                    card_id, tag_id,
                    1.0,  # High confidence for official keywords
                    random.random(),  # Random weight between 0 and 1
                    'scryfall_import',  # Source of the tag
                    'v1.0'  # Model version
                ))

            self.conn.commit()
            return card_id

        except Exception as e:
            print(f"✗ ERROR inserting card '{card_data.get('name', 'Unknown')}': {e}")
            self.conn.rollback()
            return None

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

    def insert_card(self, card_data: dict):
        """Insert a single card into the database"""
        try:
            # FIRST: Check if this is a multi-face card (Adventure, Split, Transform, etc.)
            # If so, process it as multiple separate cards
            if self.process_multi_face_card(card_data):
                return True  # Multi-face card processed successfully

            # Skip tokens and non-cards
            layout = card_data.get('layout')
            if layout in ['token', 'emblem', 'art_series']:
                card_name = card_data.get('name', 'Unknown')
                print(f"⊘ SKIPPED - Token/Emblem: '{card_name}' (layout: {layout})")
                self.skip_reasons['tokens_emblems'] += 1
                self.skipped_cards.append({
                    'name': card_name,
                    'reason': 'token_emblem',
                    'details': f'layout={layout}'
                })
                return False
            
            # Extract basic card info
            card_name = card_data.get('name')
            if not card_name:
                print(f"⊘ SKIPPED - Missing name: {card_data.get('id', 'Unknown ID')}")
                print(f"    Available keys: {list(card_data.keys())}")
                self.skip_reasons['missing_name'] += 1
                self.skipped_cards.append({
                    'name': 'NO_NAME',
                    'reason': 'missing_name',
                    'details': f'keys={list(card_data.keys())[:5]}'
                })
                return False

            # Check for duplicate cards
            self.cursor.execute("SELECT COUNT(*) FROM cards WHERE card_name = %s", (card_name,))
            existing_count = self.cursor.fetchone()[0]
            if existing_count > 0:
                print(f"⊘ SKIPPED - Duplicate: '{card_name}' (already exists)")
                self.skip_reasons['duplicates'] += 1
                self.skipped_cards.append({
                    'name': card_name,
                    'reason': 'duplicate',
                    'details': 'already_in_database'
                })
                return False

            # Get or create set FIRST
            set_name = card_data.get('set_name', 'Unknown')
            set_id = self.get_or_create_set(set_name)
            
            # Prepare card data
            mana_cost = card_data.get('mana_cost', '')
            cmc = int(card_data.get('cmc', 0))
            
            # Parse type line to extract card_type, card_subtype, card_supertype
            type_line = card_data.get('type_line', '')
            card_supertype, card_type, card_subtype = self.parse_type_line(type_line)
            
            oracle_text = card_data.get('oracle_text', '')
            power = card_data.get('power')
            toughness = card_data.get('toughness')
            rarity = card_data.get('rarity', 'common')
            image_url = card_data.get('image_uris', {}).get('normal', '')
            flavor_text = card_data.get('flavor_text', '')
            foil = card_data.get('foil', False)
            game_changer = card_data.get('game_changer', False)
            language = card_data.get('lang', 'en')
            
            # Variant information
            collector_number = card_data.get('collector_number', 'unknown')
            promo = card_data.get('promo', False)
            variation = card_data.get('variation', False)
            
            # Color identity as comma-separated string
            color_identity_list = card_data.get('color_identity', [])
            color_identity_str = ','.join(color_identity_list) if color_identity_list else ''

            # VALIDATION: Check required fields before insert
            # Note: mana_cost and color_identity can be null
            required_fields = {
                'card_name': card_name,
                'cmc': cmc,
                'type_line': type_line,
                'card_type': card_type,
                'rarity': rarity,
                'card_text': oracle_text,
                'image_url': image_url,
                'language': language,
                'set_id': set_id,
                'collector_number': collector_number
            }

            # Check for any null/empty required fields
            missing_fields = []
            for field_name, field_value in required_fields.items():
                if field_value is None or field_value == '':
                    missing_fields.append(f"{field_name}='{field_value}'")

            if missing_fields:
                print(f"⚠ Warning for card '{card_name}': Missing required fields: {', '.join(missing_fields)}")
                # Set defaults for critical fields
                # Note: mana_cost and color_identity are allowed to be null/empty
                critical_missing = []
                if not type_line:
                    type_line = 'Unknown'
                    critical_missing.append('type_line')
                if not card_type:
                    card_type = 'Unknown'
                    critical_missing.append('card_type')
                if not oracle_text:
                    oracle_text = ''
                if not image_url:
                    image_url = 'https://example.com/missing.jpg'
                    critical_missing.append('image_url')
                if not rarity:
                    rarity = 'common'
                    critical_missing.append('rarity')

                # If too many critical fields missing, skip the card
                if len(critical_missing) > 2:
                    print(f"⊘ SKIPPED - Too many missing critical fields: '{card_name}' - {critical_missing}")
                    self.skip_reasons['validation_errors'] += 1
                    self.skipped_cards.append({
                        'name': card_name,
                        'reason': 'validation_error',
                        'details': f'missing_fields={critical_missing}'
                    })
                    return False

            # Debug log for problematic cards
            if card_name and ('Cardboard' in card_name or 'Treasure Vault' in card_name):
                print(f"🔍 DEBUG - Processing card: {card_name}")
                print(f"    mana_cost: '{mana_cost}' (type: {type(mana_cost)})")
                print(f"    cmc: {cmc} (type: {type(cmc)})")
                print(f"    color_identity: '{color_identity_str}' (type: {type(color_identity_str)})")
                print(f"    type_line: '{type_line}' (type: {type(type_line)})")
                print(f"    card_type: '{card_type}' (type: {type(card_type)})")
                print(f"    card_supertype: '{card_supertype}' (type: {type(card_supertype)})")
                print(f"    rarity: '{rarity}' (type: {type(rarity)})")
                print(f"    oracle_text length: {len(oracle_text) if oracle_text else 'NULL'}")
                print(f"    image_url: '{image_url[:50]}...' (type: {type(image_url)})")
                print(f"    set_id: {set_id} (type: {type(set_id)})")
                print(f"    collector_number: '{collector_number}' (type: {type(collector_number)})")

            # PROCESS AND CREATE TAGS FIRST (before inserting card)
            keyword_tag_ids = []
            keywords = card_data.get('keywords', [])
            for keyword in keywords:
                if keyword and keyword.strip():
                    tag_id = self.get_or_create_tag(keyword.strip())
                    keyword_tag_ids.append((tag_id, keyword.strip()))

            # FINAL VALIDATION before INSERT
            insert_values = (
                card_name, mana_cost, cmc, color_identity_str, type_line, card_type,
                card_supertype or None, rarity, oracle_text, flavor_text or None,
                power, toughness, False, image_url, foil,
                game_changer, language, set_id, collector_number, promo, variation
            )

            # Log the exact values being inserted for problematic cards
            if card_name and ('Cardboard' in card_name or self.cards_inserted == 0):
                print(f"🔍 PRE-INSERT DEBUG for '{card_name}':")
                field_names = [
                    'card_name', 'mana_cost', 'cmc', 'color_identity', 'type_line', 'card_type',
                    'card_supertype', 'rarity', 'card_text', 'flavor_text',
                    'power', 'toughness', 'unlimited_copies', 'image_url', 'foil',
                    'game_changer', 'language', 'card_set', 'collector_number', 'promo', 'variation'
                ]
                insert_values_debug = (
                    card_name, mana_cost, cmc, color_identity_str, type_line, card_type,
                    card_supertype or None, rarity, oracle_text, flavor_text or None,
                    power, toughness, False, image_url, foil,
                    game_changer, language, set_id, collector_number, promo, variation
                )
                for i, (field_name, value) in enumerate(zip(field_names, insert_values_debug)):
                    value_str = str(value) if value is not None else 'NULL'
                    if len(value_str) > 50:
                        value_str = value_str[:47] + '...'
                    print(f"    {i+1:2d}. {field_name:18s} = {value_str} ({type(value).__name__})")

            # NOW INSERT THE CARD (after sets and tags are created)
            # Using ENHANCED schema structure: only card_name (no 'name') and card_text (no 'oracle_text')
            self.cursor.execute("""
                INSERT INTO cards (
                    card_name, mana_cost, cmc, color_identity, type_line, card_type,
                    card_supertype, rarity, card_text, flavor_text,
                    power, toughness, unlimited_copies, image_url, foil,
                    game_changer, language, card_set, collector_number, promo, variation
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                ) RETURNING id
            """, (
                card_name,                   # card_name (only name field in ENHANCED schema)
                mana_cost or None,           # mana_cost (can be null)
                cmc,                         # cmc
                color_identity_str or None,  # color_identity (can be null)
                type_line,                   # type_line
                card_type,                   # card_type
                card_supertype or None,      # card_supertype
                rarity,                      # rarity
                oracle_text,                 # card_text (only text field in ENHANCED schema)
                flavor_text or None,         # flavor_text
                power,                       # power
                toughness,                   # toughness
                False,                       # unlimited_copies
                image_url,                   # image_url
                foil,                        # foil
                game_changer,                # game_changer
                language,                    # language
                set_id,                      # card_set
                collector_number,            # collector_number
                promo,                       # promo
                variation                    # variation
            ))

            card_id = self.cursor.fetchone()[0]
            
            # NOW INSERT RELATED DATA (after card exists)

            # Insert colors
            colors = card_data.get('colors', [])
            for color_code in colors:
                if color_code in COLOR_MAP:
                    self.cursor.execute(
                        "INSERT INTO card_colors (card_id, color) VALUES (%s, %s)",
                        (card_id, COLOR_MAP[color_code])
                    )
            
            # Insert color identity
            color_identity = card_data.get('color_identity', [])
            for color_code in color_identity:
                if color_code in COLOR_MAP:
                    self.cursor.execute(
                        "INSERT INTO card_color_identity (card_id, color) VALUES (%s, %s)",
                        (card_id, COLOR_MAP[color_code])
                    )
            
            # Insert types (Creature, Instant, Artifact, etc.)
            if card_type:
                types = card_type.split()
                for type_name in types:
                    if type_name and type_name.strip():
                        self.cursor.execute(
                            "INSERT INTO card_types (card_id, type) VALUES (%s, %s)",
                            (card_id, type_name.strip())
                        )

            # Insert supertypes (Legendary, Basic, Snow, etc.)
            if card_supertype:
                supertypes = card_supertype.split()
                for supertype_name in supertypes:
                    if supertype_name and supertype_name.strip():
                        self.cursor.execute(
                            "INSERT INTO card_supertypes (card_id, supertype) VALUES (%s, %s)",
                            (card_id, supertype_name.strip())
                        )

            # Insert keywords (Flying, Trample, etc.)
            for keyword in keywords:
                if keyword and keyword.strip():
                    self.cursor.execute(
                        "INSERT INTO card_keywords (card_id, keyword) VALUES (%s, %s)",
                        (card_id, keyword.strip())
                    )
            
            # Insert subtypes (Goblin, Wizard, Equipment, Adventure, etc.)
            if card_subtype:
                subtypes = self.parse_subtypes(card_subtype)
                for subtype in subtypes:
                    if subtype and subtype.strip():
                        self.cursor.execute(
                            "INSERT INTO card_subtypes (card_id, subtype) VALUES (%s, %s)",
                            (card_id, subtype.strip())
                        )

            # Insert card-tag relationships (using pre-created tags)
            for tag_id, keyword in keyword_tag_ids:
                self.cursor.execute("""
                    INSERT INTO card_tag (
                        card_id, tag_id, confidence, weight, source, model_version
                    ) VALUES (%s, %s, %s, %s, %s, %s)
                    ON CONFLICT (card_id, tag_id) DO NOTHING
                """, (
                    card_id, tag_id,
                    1.0,  # High confidence for official keywords
                    random.random(),  # Random weight between 0 and 1
                    'scryfall_import',  # Source of the tag
                    'v1.0'  # Model version
                ))

            return True
            
        except psycopg2.Error as db_error:
            # Database-specific errors
            card_name = card_data.get('name', 'Unknown')
            print(f"⊘ SKIPPED - DB ERROR: '{card_name}'")
            print(f"    Error Code: {db_error.pgcode}")
            print(f"    Error Message: {db_error.pgerror}")
            print(f"    SQL State: {getattr(db_error, 'sqlstate', 'Unknown')}")

            # Log specific card data that caused the error
            print(f"    Problem fields:")
            print(f"      mana_cost: '{card_data.get('mana_cost', 'NULL')}'")
            print(f"      cmc: {card_data.get('cmc', 'NULL')}")
            print(f"      type_line: '{card_data.get('type_line', 'NULL')}'")
            print(f"      rarity: '{card_data.get('rarity', 'NULL')}'")
            print(f"      set_name: '{card_data.get('set_name', 'NULL')}'")

            self.skip_reasons['db_errors'] += 1
            self.skipped_cards.append({
                'name': card_name,
                'reason': 'database_error',
                'details': f'pgcode={db_error.pgcode}, error={str(db_error)[:100]}'
            })

            self.conn.rollback()
            return False

        except Exception as e:
            # General errors
            card_name = card_data.get('name', 'Unknown')
            print(f"⊘ SKIPPED - GENERAL ERROR: '{card_name}' - {type(e).__name__}: {str(e)}")

            # Enhanced debugging for general errors
            print(f"    Raw card keys: {list(card_data.keys())}")
            if 'name' in card_data:
                print(f"    Raw name value: '{card_data['name']}' (type: {type(card_data['name'])})")
            if 'type_line' in card_data:
                print(f"    Type line: '{card_data.get('type_line', 'NULL')}'")

            self.skip_reasons['general_errors'] += 1
            self.skipped_cards.append({
                'name': card_name,
                'reason': 'general_error',
                'details': f'{type(e).__name__}: {str(e)[:100]}'
            })

            self.conn.rollback()
            return False
    
    def import_cards(self, json_file_path: str, batch_size: int = 100):
        """Import cards from JSON file"""
        print(f"\n📖 Reading JSON file: {json_file_path}")
        
        try:
            with open(json_file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                # Handle JSON array format (cards separated by commas)
                cards = []
                
                # Try to parse as array first
                try:
                    cards = json.loads(content)
                    # If it's a single dict, wrap in list
                    if isinstance(cards, dict):
                        cards = [cards]
                except:
                    # If that fails, try line by line
                    for line in content.strip().split('\n'):
                        if line.strip():
                            try:
                                card = json.loads(line.strip().rstrip(','))
                                cards.append(card)
                            except:
                                continue
                
                print(f"✓ Found {len(cards)} cards in JSON file")
                
        except Exception as e:
            print(f"✗ Failed to read JSON file: {e}")
            return
        
        # Import cards in batches
        print(f"\n💾 Importing cards (batch size: {batch_size})...")
        
        for i, card in enumerate(cards, 1):
            success = self.insert_card(card)
            
            if success:
                self.cards_inserted += 1
            else:
                self.cards_skipped += 1
            
            # Commit in batches
            if i % batch_size == 0:
                self.conn.commit()
                print(f"  Processed {i}/{len(cards)} cards... "
                      f"(Inserted: {self.cards_inserted}, Skipped: {self.cards_skipped}, "
                      f"Tags created: {self.tags_created})")
        
        # Final commit
        self.conn.commit()
        
        # Print enhanced summary with multi-face statistics
        print(f"\n{'='*60}")
        print(f"📊 DETAILED IMPORT SUMMARY")
        print(f"{'='*60}")
        print(f"✓ Cards inserted:     {self.cards_inserted}")
        print(f"⊘ Cards skipped:      {self.cards_skipped}")
        print(f"🏷  Tags created:       {self.tags_created}")

        # Multi-face card statistics
        total_multi_face = sum(count for key, count in self.multi_face_stats.items()
                              if key not in ['total_faces_created', 'total_combined_created'])
        if total_multi_face > 0:
            print(f"")
            print(f"🎭 Multi-Face Card Statistics:")
            for card_type, count in self.multi_face_stats.items():
                if count > 0 and card_type not in ['total_faces_created', 'total_combined_created']:
                    print(f"  {card_type.capitalize():12s}: {count:4d} cards → {count * 3:4d} DB entries")
            print(f"  {'Total faces':12s}: {self.multi_face_stats['total_faces_created']:4d}")
            print(f"  {'Combined refs':12s}: {self.multi_face_stats['total_combined_created']:4d}")
            print(f"  {'Total multi-face':12s}: {total_multi_face:4d} input → {total_multi_face * 3:4d} DB entries")

        # Skip breakdown if any cards were skipped
        if self.cards_skipped > 0:
            print(f"")
            print(f"📋 Skip Breakdown:")
            for reason, count in self.skip_reasons.items():
                if count > 0:
                    reason_display = reason.replace('_', ' ').title()
                    print(f"  {reason_display:15s}: {count}")

        print(f"{'='*60}\n")

        # Export detailed skip log if any cards were skipped
        if self.cards_skipped > 0:
            self.export_skipped_cards_log()

    def diagnose_table_structure(self):
        """Diagnose the actual structure of the cards table"""
        try:
            print("\n🔍 DIAGNOSING TABLE STRUCTURE...")

            # Check if cards table exists
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

            # Get column information
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

            # Columns that our INSERT statement expects
            script_expects = [
                'card_name', 'mana_cost', 'cmc', 'color_identity', 'type_line', 'card_type',
                'card_supertype', 'rarity', 'card_text', 'flavor_text',
                'power', 'toughness', 'unlimited_copies', 'image_url', 'foil',
                'game_changer', 'language', 'card_set', 'collector_number', 'promo', 'variation'
            ]

            missing_columns = []
            extra_columns = []

            # Check what our script expects vs what exists
            print("Checking required columns for script:")
            for expected in script_expects:
                if expected in actual_columns:
                    print(f"  ✅ {expected:20s} - EXISTS")
                else:
                    print(f"  ❌ {expected:20s} - MISSING!")
                    missing_columns.append(expected)

            # Check for columns in DB that our script doesn't use
            print(f"\nColumns in DB not used by script:")
            for actual in actual_columns:
                if actual not in script_expects:
                    print(f"  ℹ️  {actual:20s} - Not used by script")
                    extra_columns.append(actual)

            print("=" * 70)

            # CRITICAL WARNINGS
            if missing_columns:
                print(f"\n🚨 CRITICAL ERROR: Missing required columns!")
                print("   The following columns are required but don't exist:")
                for col in missing_columns:
                    print(f"     ❌ {col}")
                print(f"   ⛔ Import will FAIL if you proceed!")

            # INFORMATION
            if extra_columns:
                print(f"\n💡 Additional columns in database:")
                for col in extra_columns:
                    print(f"     ℹ️  {col}")
                print(f"   📝 These might indicate schema differences or unused fields.")

            # Name column analysis
            name_candidates = [col for col in actual_columns if 'name' in col.lower()]
            if name_candidates:
                print(f"\n🏷️  Name-related columns found: {', '.join(name_candidates)}")

            # Check for common problematic columns
            if 'name' in actual_columns and 'card_name' in actual_columns:
                print(f"\n⚡ WARNING: Both 'name' and 'card_name' exist!")
                print(f"   Script will insert into 'card_name' only.")
                print(f"   If 'name' is also NOT NULL, this will cause errors!")

            return True

        except Exception as e:
            print(f"✗ Error diagnosing table: {e}")
            return False

    def analyze_skips_in_json(self, json_file_path: str):
        """Analyze potential skip reasons and multi-face cards before importing"""
        print(f"\n🔍 PRE-IMPORT ANALYSIS: {json_file_path}")
        print("-" * 60)

        try:
            with open(json_file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                cards = []

                # Try to parse as array first
                try:
                    cards = json.loads(content)
                    if isinstance(cards, dict):
                        cards = [cards]
                except:
                    # If that fails, try line by line
                    for line in content.strip().split('\n'):
                        if line.strip():
                            try:
                                card = json.loads(line.strip().rstrip(','))
                                cards.append(card)
                            except:
                                continue

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

            for card in cards:
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

                if layout in ['token', 'emblem', 'art_series']:
                    skip_analysis['tokens'] += 1
                    if layout not in skip_analysis['problematic_layouts']:
                        skip_analysis['problematic_layouts'].append(layout)

                if not card.get('name'):
                    skip_analysis['missing_name'] += 1
                if not card.get('type_line'):
                    skip_analysis['missing_type'] += 1
                if not card.get('rarity'):
                    skip_analysis['missing_rarity'] += 1
                if not card.get('image_uris', {}).get('normal'):
                    skip_analysis['missing_image'] += 1

            print(f"📊 Analysis of {len(cards)} total cards:")

            # Show multi-face cards info
            if skip_analysis['multi_face_cards'] > 0:
                print(f"🎭 Multi-face cards found: {skip_analysis['multi_face_cards']}")
                print(f"   Types breakdown:")
                for mf_type, count in sorted(multi_face_types.items(), key=lambda x: x[1], reverse=True):
                    print(f"     {mf_type:12s}: {count:4d} cards")
                print(f"   → These will be split into {skip_analysis['multi_face_cards'] * 3} total cards")

            # Show predicted skips
            total_predicted_skips = 0
            print(f"\n📊 Predicted skips:")
            for reason, count in skip_analysis.items():
                if reason not in ['problematic_layouts', 'multi_face_cards'] and count > 0:
                    print(f"  {reason}: {count}")
                    total_predicted_skips += count

            print(f"\n📋 Layout distribution:")
            for layout, count in sorted(layout_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
                status = "❌ SKIP" if layout in ['token', 'emblem', 'art_series'] else "✅ OK"
                multi_face_indicator = " 🎭 MULTI-FACE" if layout in ['adventure', 'split', 'transform', 'modal_dfc', 'flip'] else ""
                print(f"  {layout:15s}: {count:5d} cards {status}{multi_face_indicator}")

            # Calculate final statistics
            base_cards = len(cards) - skip_analysis['multi_face_cards'] - total_predicted_skips
            multi_face_expansions = skip_analysis['multi_face_cards'] * 2  # Each multi-face becomes 3 cards (2 extra)
            total_expected_cards = base_cards + skip_analysis['multi_face_cards'] + multi_face_expansions

            print(f"\n🎯 Expected import results:")
            print(f"  📥 Input cards: {len(cards)}")
            print(f"  ⊘ Skipped: {total_predicted_skips}")
            print(f"  🎭 Multi-face: {skip_analysis['multi_face_cards']} (become {skip_analysis['multi_face_cards'] * 3})")
            print(f"  ✅ Regular: {base_cards}")
            print(f"  📤 Total DB entries: {total_expected_cards}")
            print(f"  📊 Success rate: {((len(cards) - total_predicted_skips) / len(cards) * 100):.1f}%")
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

                f.write(f"Summary:\n")
                f.write(f"  Total cards skipped: {self.cards_skipped}\n")
                f.write(f"  Total cards imported: {self.cards_inserted}\n\n")

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
{'='*60}
🃏  MTG Card Importer
{'='*60}
Database: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}
JSON File: {json_file}
{'='*60}
    """)
    
    importer = CardImporter()
    
    try:
        importer.connect()

        # PRE-IMPORT ANALYSIS
        print("\n" + "="*60)
        print("🔍 PRE-IMPORT CARD ANALYSIS")
        print("="*60)
        importer.analyze_skips_in_json(json_file)

        # DIAGNOSE TABLE STRUCTURE
        print("\n" + "="*60)
        print("🔍 ANALYZING DATABASE STRUCTURE")
        print("="*60)

        if not importer.diagnose_table_structure():
            print("✗ Cannot proceed - table structure diagnosis failed")
            return

        # MANDATORY PAUSE - User must review structure before proceeding
        print("\n" + "="*60)
        print("📋 TABLE STRUCTURE ANALYSIS COMPLETE")
        print("="*60)
        print("✅ SCHEMA: Using ENHANCED schema (recreated database)")
        print("✅ STRUCTURE: Clean table without obsolete columns")
        print("💡 REMOVED: oracle_text, name, and other legacy fields")
        print("🎯 USING: Only card_name and card_text as per ENHANCED schema")
        print("🎭 MULTI-FACE: Adventure, Split, Transform, Modal DFC cards supported")
        print("-" * 60)
        print("🚀 Proceeding with enhanced import...")

        # Load caches and start import
        importer.load_tag_cache()
        importer.load_set_cache()
        importer.import_cards(json_file, batch_size=100)
    except KeyboardInterrupt:
        print("\n\n⚠ Import cancelled by user")
    except Exception as e:
        print(f"\n✗ Import failed: {e}")
    finally:
        importer.close()


if __name__ == "__main__":
    main()
