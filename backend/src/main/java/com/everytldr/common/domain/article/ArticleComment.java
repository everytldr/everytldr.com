package com.everytldr.common.domain.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.everytldr.common.domain.support.SoftDeletableEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_article_comment_article_id", columnList = "article_id, id"))
public class ArticleComment extends SoftDeletableEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_article_comment_article"))
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_article_comment_parent"))
  private ArticleComment parent;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(columnDefinition = "CHAR(60) NOT NULL")
  private String passwordHash;

  @Column(columnDefinition = "CHAR(64) NOT NULL")
  private String ipHash;

  @Column(nullable = false, length = 32)
  private String maskedIp;

  @Column(columnDefinition = "TEXT NOT NULL")
  private String content;

  private ArticleComment(
      Article article,
      ArticleComment parent,
      String nickname,
      String passwordHash,
      String ipHash,
      String maskedIp,
      String content) {
    this.article = article;
    this.parent = parent;
    this.nickname = nickname;
    this.passwordHash = passwordHash;
    this.ipHash = ipHash;
    this.maskedIp = maskedIp;
    this.content = content;
  }

  public static ArticleComment createTopLevel(
      Article article,
      String nickname,
      String passwordHash,
      String ipHash,
      String maskedIp,
      String content) {
    return new ArticleComment(article, null, nickname, passwordHash, ipHash, maskedIp, content);
  }

  public static ArticleComment createReply(
      Article article,
      ArticleComment parent,
      String nickname,
      String passwordHash,
      String ipHash,
      String maskedIp,
      String content) {
    return new ArticleComment(article, parent, nickname, passwordHash, ipHash, maskedIp, content);
  }
}
