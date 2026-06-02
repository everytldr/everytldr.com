package com.everytldr.api.support.error;

import com.everytldr.api.article.ArticleCommentExceptions;
import com.everytldr.api.article.ArticleExceptions;
import com.everytldr.api.support.client.ClientAddressExceptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ArticleExceptions.NotFound.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  void handleArticleNotFound() {}

  @ExceptionHandler(ArticleCommentExceptions.InvalidParent.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  void handleInvalidCommentParent() {}

  @ExceptionHandler(ClientAddressExceptions.Unavailable.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  void handleClientAddressUnavailable() {}
}
