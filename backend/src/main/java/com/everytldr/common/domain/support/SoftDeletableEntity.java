package com.everytldr.common.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public abstract class SoftDeletableEntity extends BaseEntity {
  @Column private Instant deletedAt;

  public void softDelete(Instant at) {
    this.deletedAt = at;
  }

  public boolean isSoftDeleted() {
    return deletedAt != null;
  }
}
