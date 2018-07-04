package io.github.scottleedavis.mattermost.remind.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class FormatterException extends Exception {
    public FormatterException(String message) {
        super(message);
    }
}
