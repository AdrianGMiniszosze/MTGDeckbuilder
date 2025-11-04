insert into sets (name)
values ('Ravnica Allegiance'),
('Blumburrow'),
('The Lord of the Rings: Tales of Middle-earth'),
('Phyrexia: All Will Be One'),
('Kaldheim'),
('Modern Horizons'),
('Ikoria: Lair of Behemoths'),
('Outlaws of Thunder Junction Commander'),
('Tarkir: Dragonstorm Commander'),
('Duskmourn: House of Horror Commander'),
('Fourth Edition'),
('Dragon Maze'),
('Masters 25'),
('Modern Masters');


INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, cardSuperType, rarity, cardSet, cardText, 
flavorText, power, toughness, unlimitedCopies, cardImage) 
VALUES ('Persistent Petitioners', '1U', 'BLUE', 'Creature', 'Human', 'Advisor', 'Rare', 'Ravnica Allegiance', 
    'Mill a card.', 'Persistent Petitioners is always watching.', 1, 3, true, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Bellowing Crier', '1U', 'BLUE', 'Creature', 'Frog Advisor', 'Common', 'Blumburrow', 
'When this creatures enters, draw a card, then discard a card.', 
2, 1, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Faerie Macabre', '1BB', 'BLACK', 'Creature', 'Faerie Rogue', 'Common', 'Dual Decks Anthology: Garruk vs Liliana', 
'Flying. Discard: Faerie Macabre: Exile up to two target cards from graveyards.', 
2, 2, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Generous Ent', '5G', 'GREEN', 'Creature', 'Treefolk', 'Common', 'The Lord of the Ringd: Tales of Middle-earth', 
'Reach. When Generous Ent enters the battlefield, create a Food Token. Forestcycling 1', 
5, 7, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Glistener Seer', 'U', 'BLUE', 'Creature', 'Phyrexia Advisor', 'Common', 'Phyrexia: All Will Be One', 
'Glistener Seer enters the battlefield with three oil counters on it. T, Remove an oil counter from Glistener Seer: Scry 1.', 
0, 3, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Masked Vandal', '1G', 'GREEN', 'Creature', 'Shapeshifter', 'Common', 'Kaldheim', 
'Changeling. When Masked Vandal enters the battlefield, you may exile a creature card from your graveyard. If you do, exile target artifact or enchantment an opponent controls.', 
1, 3, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Pond Prophet', 'U/GU/G', 'SIMIC', 'Creature', 'Frog Advisor', 'Common', 'Blumburrow', 
'When Pond Prophet enters, draw a card.', 
1, 1, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Sibsig Appraiser', '2U', 'BLUE', 'Creature', 'Zombie Advisor', 'Common', 'Tarkir: Dragonstorm', 
'When this creatures enters, look at the top two cards of your library. Put one of them into your hand and te other into your graveyard', 
2, 1, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardSubType, cardsupertype,rarity, cardSet, cardText, 
power, toughness, unlimitedCopies, cardImage) 
VALUES ('Universal Automaton', '1', 'COLORLESS', 'Creature', 'Shapeshifter', 'Artifact', 'Common', 'Modern Horizons', 
'Changeling.', 
1, 1, false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Lead the Stampede', '2G', 'GREEN', 'Sorcery', 'Common', 'Ikoria: Lair of Behemoths', 
'Look at the top five cards of your library. You may reveal any number of creature cards from among them and put the revealed cards into your hand. Put the rest on the bottom of your library in any order.', 
false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Winding Way', '1G', 'GREEN', 'Sorcery', 'Common', 'Outlaws of Thunder Junction Commander', 
'Chose creature or land. Reveal the top four cards of your library. Put all cards of the chosen type revealed this way into your hand and the rest into your graveyard', 
false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Bojuka Bog', '0', 'BLACK', 'Land', 'Common', 'Tarkir: Dragonstorm Commander', 
'This land enters tapped. When this land enters, exile target players graveyard. T: Add B', false, 'image_url');


INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardsupertype ,rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Forest', '0', 'GREEN', 'Land', 'Basic', 'Common', 'Tarkir: Dragonstorm', 
'T: Add G', true, 'image_url');


INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardsupertype ,rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Island', '0', 'BLUE', 'Land', 'Basic', 'Common', 'Tarkir: Dragonstorm', 
'T: Add U', true, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Simic Growth Chamber', '0', 'SIMIC', 'Land', 'Common', 'Duskmourn: House of Horror Commander', 
'Simic Growth Chamber enters tapped. When Simic Growth Chamber enters, return a land you control to its owner hand T: Add GU.', 
false, 'image_url');


INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardsupertype, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Woodland Chasm', '0', 'SIMIC', 'Land', 'Snow', 'Common', 'Kaldheim', 
'T: Add B or G. Woodland Chasm enters the battlefield tapped', false, 'image_url');


INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Blue Elemental Blast', 'U', 'BLUE', 'Instant', 'Common', 'Fourth Edition', 
'Counter target red spell or destroy target red permanent', false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Crypt Incursion', '2B', 'BLACK', 'Instant', 'Common', 'Dragon Maze', 
'Exile all creature cards from target player graveyard. You gain 3 life for each card exiled this way', false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Heritage Reclamation', '1G', 'GREEN', 'Instant', 'Common', 'Tarkir: Dragonstorm', 
'Choose one - * Destroy target artifact. * Destroy target enchantment. * Exile up to one target card from a graveyard. Draw a card.', 
false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Nihil Spellbomb', '1', 'BLACK', 'Artifact', 'Common', 'Masters 25', 
'T, Sacrifice Nihil Spellbomb: Exile all cards from target player graveyard. When Nihil Spellbomb is put into a graveyard from the battlefield, you may pay B. If you do, draw a card.', 
false, 'image_url');

INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, cardsubtype, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Spellstutter Sprite', '1U', 'BLUE', 'Creature', 'Faerie Wizard', 'Common', 'Modern Masters', 
'Flash. Flying, When Spellstutter Sprite enters the battlefield, counter target spell with converted mana cost X or less, where X is the number of Faeries you control.', 
false, 'image_url');


INSERT INTO cards 
(cardName, manaCost, colorIdentity, cardType, rarity, cardSet, cardText, unlimitedCopies, cardImage) 
VALUES ('Weather the Storm', '1G', 'GREEN', 'Instant', 'Common','Modern Horizons', 
'You gain 3 life. Storm', false, 'image_url');


INSERT INTO formats (formatName, deckSize)
VALUES ('Pauper', 60);

INSERT INTO card_legality (cardId, formatId, legalityStatus) VALUES
(1, 1, 'legal'),
(2, 1, 'legal'),
(3, 1, 'legal'),
(4, 1, 'legal'),
(5, 1, 'legal'),
(6, 1, 'legal'),
(7, 1, 'legal'),
(8, 1, 'legal'),
(9, 1, 'legal'),
(10, 1, 'legal'),
(11, 1, 'legal'),
(12, 1, 'legal'),
(13, 1, 'legal'),
(14, 1, 'legal'),
(15, 1, 'legal'),
(16, 1, 'legal'),
(17, 1, 'legal'),
(18, 1, 'legal'),
(19, 1, 'legal'),
(20, 1, 'legal'),
(21, 1, 'legal'),
(22, 1, 'legal');

INSERT INTO users (name, username, hashed_password, email, country)
VALUES ('Adrian', 'miniszosze', 'hashed_pw_here', 'adrian@example.com', 'Spain');

INSERT INTO decks (deckName, description, private, decktype ,tournament, shareURL, format, userId)
VALUES ('Persistent Petitioners Pauper', 'A sample Pauper deck', true, 'main', NULL, 'unique-share-url-123', 1, 1);

INSERT INTO decks (deckName, description, private, decktype ,tournament, shareURL, format, userId, parent_deck_id)
VALUES ('Persistent Petitioners Pauper', 'A sample Pauper deck', true, 'sideboard', NULL, 'unique-share-url-456', 1, 1, 1);

INSERT INTO decks (deckName, description, private, decktype ,tournament, shareURL, format, userId, parent_deck_id)
VALUES ('Persistent Petitioners Pauper', 'A sample Pauper deck', true, 'maybeboard', NULL, 'unique-share-url-789', 1, 1, 1);

insert into card_deck (cardid, deckid, quantity)
values (1,1,11),(2,1,4),(3,1,2),(4,1,4),(5,1,4),(6,1,3),(7,1,4),(8,1,2),(9,1,3),(10,1,4),(11,1,2),(12,1,2),(13,1,2),(14,1,8),(15,1,4),(16,1,1);

insert into card_deck(cardid, deckid, quantity)
values (17,2,2),(18,2,3),(19,2,2),(20,2,3),(21,2,3),(22,2,2);