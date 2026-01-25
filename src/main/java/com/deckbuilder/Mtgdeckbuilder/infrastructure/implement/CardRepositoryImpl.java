package com.deckbuilder.mtgdeckbuilder.infrastructure.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepositoryCustom;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom implementation of CardRepository using EntityManager for dynamic queries
 */
@Repository
@Slf4j
public class CardRepositoryImpl implements CardRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<CardEntity> searchCardsWithDetailedCriteria(CardSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching cards with criteria: {}", criteria);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Query for actual results
        CriteriaQuery<CardEntity> query = cb.createQuery(CardEntity.class);
        Root<CardEntity> cardRoot = query.from(CardEntity.class);

        // Build predicates dynamically
        List<Predicate> predicates = buildPredicates(cb, cardRoot, criteria);

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        applySorting(cb, query, cardRoot, criteria);

        // Create typed query with pagination
        TypedQuery<CardEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<CardEntity> results = typedQuery.getResultList();

        // Get total count
        long total = getTotalCount(criteria);

        log.debug("Found {} cards out of {} total", results.size(), total);

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public List<CardEntity> findRandomCards(int count, String type, String rarity, Long formatId) {
        log.debug("Finding {} random cards with type={}, rarity={}, formatId={}", count, type, rarity, formatId);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CardEntity> query = cb.createQuery(CardEntity.class);
        Root<CardEntity> cardRoot = query.from(CardEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        // Add type filter
        if (type != null && !type.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(cardRoot.get("cardType")),
                "%" + type.toLowerCase() + "%"));
        }

        // Add rarity filter
        if (rarity != null && !rarity.trim().isEmpty()) {
            predicates.add(cb.equal(cb.lower(cardRoot.get("rarity")),
                rarity.toLowerCase()));
        }

        // Add format filter (requires join with card_legality)
        if (formatId != null) {
            // For format filtering, we'll use a native query as it's more complex
            return findRandomCardsWithFormatNative(count, type, rarity, formatId);
        }

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // For random ordering, we'll use native query
        String sql = buildRandomQuerySql(type, rarity, formatId, count);

        @SuppressWarnings("unchecked")
        List<CardEntity> results = entityManager.createNativeQuery(sql, CardEntity.class)
            .setParameter("count", count)
            .getResultList();

        log.debug("Found {} random cards", results.size());
        return results;
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<CardEntity> cardRoot, CardSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        // Name filter (maps to card_name column)
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(cardRoot.get("name")),
                "%" + criteria.getName().toLowerCase() + "%"));
        }

        // Type filter (maps to card_type column)
        if (criteria.getType() != null && !criteria.getType().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(cardRoot.get("cardType")),
                "%" + criteria.getType().toLowerCase() + "%"));
        }

        // Rarity filter
        if (criteria.getRarity() != null && !criteria.getRarity().trim().isEmpty()) {
            predicates.add(cb.equal(cb.lower(cardRoot.get("rarity")),
                criteria.getRarity().toLowerCase()));
        }

        // Color identity filter (maps to color_identity column)
        if (criteria.getColors() != null && !criteria.getColors().trim().isEmpty()) {
            predicates.add(cb.like(cardRoot.get("colorIdentity"),
                "%" + criteria.getColors() + "%"));
        }

        // CMC range filters
        if (criteria.getCmcMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(cardRoot.get("cmc"), criteria.getCmcMin()));
        }
        if (criteria.getCmcMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(cardRoot.get("cmc"), criteria.getCmcMax()));
        }

        // Power range filters (with null checks)
        if (criteria.getPowerMin() != null) {
            try {
                int powerMin = Integer.parseInt(criteria.getPowerMin());
                predicates.add(cb.and(
                    cb.isNotNull(cardRoot.get("power")),
                    cb.greaterThanOrEqualTo(cardRoot.get("power"), criteria.getPowerMin())
                ));
            } catch (NumberFormatException e) {
                log.debug("Invalid power min value: {}", criteria.getPowerMin());
            }
        }
        if (criteria.getPowerMax() != null) {
            try {
                int powerMax = Integer.parseInt(criteria.getPowerMax());
                predicates.add(cb.and(
                    cb.isNotNull(cardRoot.get("power")),
                    cb.lessThanOrEqualTo(cardRoot.get("power"), criteria.getPowerMax())
                ));
            } catch (NumberFormatException e) {
                log.debug("Invalid power max value: {}", criteria.getPowerMax());
            }
        }

        // Toughness range filters (with null checks)
        if (criteria.getToughnessMin() != null) {
            try {
                int toughnessMin = Integer.parseInt(criteria.getToughnessMin());
                predicates.add(cb.and(
                    cb.isNotNull(cardRoot.get("toughness")),
                    cb.greaterThanOrEqualTo(cardRoot.get("toughness"), criteria.getToughnessMin())
                ));
            } catch (NumberFormatException e) {
                log.debug("Invalid toughness min value: {}", criteria.getToughnessMin());
            }
        }
        if (criteria.getToughnessMax() != null) {
            try {
                int toughnessMax = Integer.parseInt(criteria.getToughnessMax());
                predicates.add(cb.and(
                    cb.isNotNull(cardRoot.get("toughness")),
                    cb.lessThanOrEqualTo(cardRoot.get("toughness"), criteria.getToughnessMax())
                ));
            } catch (NumberFormatException e) {
                log.debug("Invalid toughness max value: {}", criteria.getToughnessMax());
            }
        }

        // Set ID filter (maps to card_set column)
        if (criteria.getSetId() != null) {
            predicates.add(cb.equal(cardRoot.get("cardSet"), criteria.getSetId()));
        }

        // Text contains filter (maps to card_text column)
        if (criteria.getTextContains() != null && !criteria.getTextContains().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(cardRoot.get("cardText")),
                "%" + criteria.getTextContains().toLowerCase() + "%"));
        }

        // Foil filter
        if (criteria.getIsFoil() != null) {
            predicates.add(cb.equal(cardRoot.get("foil"), criteria.getIsFoil()));
        }

        // Promo filter
        if (criteria.getIsPromo() != null) {
            predicates.add(cb.equal(cardRoot.get("promo"), criteria.getIsPromo()));
        }

        // Language filter
        if (criteria.getLanguage() != null && !criteria.getLanguage().trim().isEmpty()) {
            predicates.add(cb.equal(cardRoot.get("language"), criteria.getLanguage()));
        }

        // Format filter (requires join with card_legality) - handled separately for complexity
        if (criteria.getFormatId() != null) {
            // This will be handled by a separate native query or subquery
            log.debug("Format filtering will be handled separately");
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<CardEntity> query, Root<CardEntity> cardRoot, CardSearchCriteria criteria) {
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "name";
        String sortOrder = criteria.getSortOrder() != null ? criteria.getSortOrder() : "asc";

        Order order;
        switch (sortBy.toLowerCase()) {
            case "cmc":
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("cmc")) : cb.asc(cardRoot.get("cmc"));
                break;
            case "rarity":
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("rarity")) : cb.asc(cardRoot.get("rarity"));
                break;
            case "type":
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("cardType")) : cb.asc(cardRoot.get("cardType"));
                break;
            case "power":
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("power")) : cb.asc(cardRoot.get("power"));
                break;
            case "toughness":
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("toughness")) : cb.asc(cardRoot.get("toughness"));
                break;
            case "set":
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("cardSet")) : cb.asc(cardRoot.get("cardSet"));
                break;
            case "name":
            default:
                order = "desc".equals(sortOrder) ? cb.desc(cardRoot.get("name")) : cb.asc(cardRoot.get("name"));
                break;
        }

        query.orderBy(order);
    }

    private long getTotalCount(CardSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CardEntity> cardRoot = countQuery.from(CardEntity.class);

        countQuery.select(cb.countDistinct(cardRoot.get("id")));

        List<Predicate> predicates = buildPredicates(cb, cardRoot, criteria);

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private String buildRandomQuerySql(String type, String rarity, Long formatId, int count) {
        StringBuilder sql = new StringBuilder("SELECT c.* FROM cards c ");

        if (formatId != null) {
            sql.append("LEFT JOIN card_legality cl ON c.id = cl.card_id ");
        }

        sql.append("WHERE 1=1 ");

        if (type != null && !type.trim().isEmpty()) {
            sql.append("AND LOWER(c.card_type) LIKE LOWER(CONCAT('%', '").append(type).append("', '%')) ");
        }

        if (rarity != null && !rarity.trim().isEmpty()) {
            sql.append("AND LOWER(c.rarity) = LOWER('").append(rarity).append("') ");
        }

        if (formatId != null) {
            sql.append("AND cl.format_id = ").append(formatId).append(" ");
        }

        sql.append("ORDER BY RANDOM() LIMIT :count");

        return sql.toString();
    }

    private List<CardEntity> findRandomCardsWithFormatNative(int count, String type, String rarity, Long formatId) {
        StringBuilder sql = new StringBuilder("SELECT c.* FROM cards c ");
        sql.append("LEFT JOIN card_legality cl ON c.id = cl.card_id ");
        sql.append("WHERE 1=1 ");

        if (type != null && !type.trim().isEmpty()) {
            sql.append("AND LOWER(c.card_type) LIKE LOWER(?) ");
        }

        if (rarity != null && !rarity.trim().isEmpty()) {
            sql.append("AND LOWER(c.rarity) = LOWER(?) ");
        }

        sql.append("AND cl.format_id = ? ");
        sql.append("ORDER BY RANDOM() LIMIT ?");

        jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString(), CardEntity.class);

        int paramIndex = 1;
        if (type != null && !type.trim().isEmpty()) {
            query.setParameter(paramIndex++, "%" + type + "%");
        }
        if (rarity != null && !rarity.trim().isEmpty()) {
            query.setParameter(paramIndex++, rarity);
        }
        query.setParameter(paramIndex++, formatId);
        query.setParameter(paramIndex, count);

        @SuppressWarnings("unchecked")
        List<CardEntity> results = query.getResultList();
        return results;
    }
}
