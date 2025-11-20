package com.platform.repository;

import com.platform.domain.ApiKey;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ApiKeyRepository implements PanacheRepositoryBase<ApiKey, UUID> {

    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return find("keyHash", keyHash).firstResultOptional();
    }

    public Optional<ApiKey> findValidByKeyHash(String keyHash) {
        return find("keyHash = ?1 and active = true", keyHash)
                .firstResultOptional()
                .filter(ApiKey::isValid);
    }
}
