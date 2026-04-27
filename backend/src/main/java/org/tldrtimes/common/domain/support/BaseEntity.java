package org.tldrtimes.common.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
  @Id @SnowflakeId private Long id;

  @Column @LastModifiedDate private Instant updatedAt;

  @Transient private Instant createdAt;

  @PostLoad
  @PostPersist
  private void deriveCreatedAt() {
    this.createdAt = (id == null) ? null : SnowflakeIdGenerator.extractTimestamp(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }

    Class<?> thisClass = Hibernate.getClass(this);
    Class<?> otherClass = Hibernate.getClass(obj);
    if (!thisClass.equals(otherClass)) {
      return false;
    }

    BaseEntity that = (BaseEntity) obj;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Hibernate.getClass(this).hashCode();
  }
}
