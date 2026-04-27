package org.tldrtimes.common.domain.source;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RssSourceRepository extends JpaRepository<RssSource, Long> {

    List<RssSource> findAllByIsActiveTrue();
}
