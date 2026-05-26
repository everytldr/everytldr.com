package org.everytldr.common.domain.support;

import java.lang.reflect.Member;
import java.util.EnumSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

public class HibernateSnowflakeIdGenerator implements BeforeExecutionGenerator {
  private final SnowflakeIdGenerator delegate;

  public HibernateSnowflakeIdGenerator(
      SnowflakeId annotation, Member member, GeneratorCreationContext context) {
    this.delegate =
        context
            .getServiceRegistry()
            .requireService(ManagedBeanRegistry.class)
            .getBean(SnowflakeIdGenerator.class)
            .getBeanInstance();
  }

  @Override
  public Object generate(
      SharedSessionContractImplementor session,
      Object owner,
      Object currentValue,
      EventType eventType) {
    return delegate.generateId();
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EventTypeSets.INSERT_ONLY;
  }
}
