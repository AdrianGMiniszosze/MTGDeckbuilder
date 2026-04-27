#!/usr/bin/env python3
"""
Analyze duplicate cards in the MTG JSON file.
This script identifies cards that appear multiple times with the same name and set,
and explains why they're considered duplicates.
"""

import json
from collections import defaultdict, Counter
from datetime import datetime


def analyze_duplicates(json_file_path: str, output_file: str = 'duplicate_analysis.txt'):
    """Analyze duplicate cards and save report to file"""
    
    print(f"Loading JSON file: {json_file_path}")
    with open(json_file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Skip tokens, emblems, art_series like the import script does
    skipped_layouts = ['token', 'emblem', 'art_series']
    cards = [c for c in data if c.get('layout') not in skipped_layouts]
    
    print(f"Analyzing {len(cards)} cards (after filtering tokens/emblems/art)...")
    
    # Group by (name, set) combination
    card_groups = defaultdict(list)
    for card in cards:
        name = card.get('name', '')
        set_name = card.get('set_name', 'Unknown')
        key = (name, set_name)
        card_groups[key].append(card)
    
    # Find duplicates
    duplicates = {k: v for k, v in card_groups.items() if len(v) > 1}
    
    # Generate report
    with open(output_file, 'w', encoding='utf-8') as out:
        out.write("=" * 100 + "\n")
        out.write("MTG CARD DUPLICATE ANALYSIS REPORT\n")
        out.write("=" * 100 + "\n")
        out.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        out.write(f"Source file: {json_file_path}\n")
        out.write("=" * 100 + "\n\n")
        
        # Summary statistics
        out.write("SUMMARY STATISTICS\n")
        out.write("-" * 100 + "\n")
        out.write(f"Total cards in JSON: {len(data)}\n")
        out.write(f"Filtered out (tokens/emblems/art): {len(data) - len(cards)}\n")
        out.write(f"Cards analyzed: {len(cards)}\n")
        out.write(f"Unique card+set combinations: {len(card_groups)}\n")
        out.write(f"Combinations with duplicates: {len(duplicates)}\n")
        out.write(f"Total duplicate entries (skipped by import): {sum(len(v) - 1 for v in duplicates.values())}\n")
        out.write("\n")
        
        # Analyze reasons for duplicates
        out.write("DUPLICATE REASONS ANALYSIS\n")
        out.write("-" * 100 + "\n")
        reasons = Counter()
        for (name, set_name), cards_list in duplicates.items():
            collector_nums = set(c.get('collector_number', '') for c in cards_list)
            if len(collector_nums) > 1:
                reasons['Different collector numbers'] += 1
            
            languages = set(c.get('lang', 'en') for c in cards_list)
            if len(languages) > 1:
                reasons['Different languages'] += 1
            
            finishes = set(str(sorted(c.get('finishes', []))) for c in cards_list)
            if len(finishes) > 1:
                reasons['Different finishes (foil/nonfoil)'] += 1
            
            promos = set(c.get('promo', False) for c in cards_list)
            if len(promos) > 1:
                reasons['Mix of promo and non-promo'] += 1
            
            variations = set(c.get('variation', False) for c in cards_list)
            if len(variations) > 1:
                reasons['Mix of variation and regular'] += 1
        
        for reason, count in reasons.most_common():
            out.write(f"  {reason}: {count} card+set combinations\n")
        out.write("\n")
        
        # Basic lands analysis
        out.write("BASIC LANDS ANALYSIS\n")
        out.write("-" * 100 + "\n")
        basic_lands = ['Plains', 'Island', 'Swamp', 'Mountain', 'Forest', 'Wastes']
        basic_dupes = {k: v for k, v in duplicates.items() if k[0] in basic_lands}
        basic_dupe_count = sum(len(v) - 1 for v in basic_dupes.values())
        non_basic_dupe_count = sum(len(v) - 1 for v in duplicates.values()) - basic_dupe_count
        
        out.write(f"Basic land duplicates: {len(basic_dupes)} combinations\n")
        out.write(f"  Extra entries skipped: {basic_dupe_count}\n")
        out.write(f"Non-basic card duplicates: {len(duplicates) - len(basic_dupes)} combinations\n")
        out.write(f"  Extra entries skipped: {non_basic_dupe_count}\n")
        out.write("\n")
        
        # Top 50 most duplicated cards
        out.write("TOP 50 MOST DUPLICATED CARDS\n")
        out.write("=" * 100 + "\n\n")
        
        sorted_dupes = sorted(duplicates.items(), key=lambda x: len(x[1]), reverse=True)
        for i, ((name, set_name), cards_list) in enumerate(sorted_dupes[:50], 1):
            out.write(f"{i}. {name}\n")
            out.write(f"   Set: {set_name}\n")
            out.write(f"   Duplicate count: {len(cards_list)} versions (keeping 1, skipping {len(cards_list)-1})\n")
            
            # Show what makes them different
            collectors = [c.get('collector_number', 'N/A') for c in cards_list]
            languages = set(c.get('lang', 'en') for c in cards_list)
            foils = [c.get('foil', False) for c in cards_list]
            nonfoils = [c.get('nonfoil', False) for c in cards_list]
            variations = [c.get('variation', False) for c in cards_list]
            promos = [c.get('promo', False) for c in cards_list]
            
            out.write(f"   Collector Numbers: {', '.join(collectors[:10])}")
            if len(collectors) > 10:
                out.write(f" ... ({len(collectors)} total)")
            out.write("\n")
            
            out.write(f"   Languages: {', '.join(sorted(languages))}\n")
            out.write(f"   Has foil: {any(foils)}, Has non-foil: {any(nonfoils)}\n")
            out.write(f"   Has promo: {any(promos)}, Has variation: {any(variations)}\n")
            out.write("\n")
        
        # Detailed breakdown by set
        out.write("\n" + "=" * 100 + "\n")
        out.write("DUPLICATES BY SET\n")
        out.write("=" * 100 + "\n\n")
        
        set_dupes = defaultdict(list)
        for (name, set_name), cards_list in duplicates.items():
            set_dupes[set_name].append((name, len(cards_list)))
        
        for set_name in sorted(set_dupes.keys(), key=lambda s: sum(count for _, count in set_dupes[s]), reverse=True)[:20]:
            cards_in_set = set_dupes[set_name]
            total_dupes = sum(count - 1 for _, count in cards_in_set)
            out.write(f"\nSet: {set_name}\n")
            out.write(f"  Cards with duplicates: {len(cards_in_set)}\n")
            out.write(f"  Total duplicate entries: {total_dupes}\n")
            out.write(f"  Top duplicated cards in this set:\n")
            for name, count in sorted(cards_in_set, key=lambda x: x[1], reverse=True)[:5]:
                out.write(f"    - {name}: {count} versions\n")
        
        out.write("\n" + "=" * 100 + "\n")
        out.write("END OF REPORT\n")
        out.write("=" * 100 + "\n")
    
    print(f"\nAnalysis complete!")
    print(f"Report saved to: {output_file}")
    print(f"\nSummary:")
    print(f"  Total cards analyzed: {len(cards)}")
    print(f"  Unique card+set combinations: {len(card_groups)}")
    print(f"  Cards imported: {len(card_groups) - sum(len(v) - 1 for v in duplicates.values())}")
    print(f"  Duplicates skipped: {sum(len(v) - 1 for v in duplicates.values())}")


if __name__ == '__main__':
    analyze_duplicates(
        'default-cards-20251101090854.json',
        'duplicate_analysis_report.txt'
    )
