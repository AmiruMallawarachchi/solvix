package com.solvix.backend.infrastructure.ticket;

import com.solvix.backend.application.ticket.TicketRepository;
import com.solvix.backend.domain.ticket.Ticket;
import com.solvix.backend.domain.ticket.TicketPriority;
import com.solvix.backend.domain.ticket.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTicketRepository implements TicketRepository {
    private final SpringDataTicketRepository delegate;

    public JpaTicketRepository(SpringDataTicketRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = TicketEntity.fromDomain(ticket);
        return delegate.save(entity).toDomain();
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        return delegate.findById(id).map(TicketEntity::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return delegate.findAll().stream().map(TicketEntity::toDomain).toList();
    }

    @Override
    public Page<Ticket> findPaged(Pageable pageable, String search, TicketStatus status, TicketPriority priority, String category, String createdBy) {
        Specification<TicketEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase().trim()));
            }
            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase().trim() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), term);
                Predicate descLike = cb.like(cb.lower(root.get("description")), term);
                predicates.add(cb.or(titleLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return delegate.findAll(spec, pageable).map(TicketEntity::toDomain);
    }
}
