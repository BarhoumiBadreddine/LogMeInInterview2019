package com.logmein.interview.badreddinesDemo.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;

@ToString
@AllArgsConstructor
@Getter
public class AppException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1L;
	private final int status;
	private final String message;
}
