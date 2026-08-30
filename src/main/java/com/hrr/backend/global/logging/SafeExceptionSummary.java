package com.hrr.backend.global.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class SafeExceptionSummary {

	private static final String APPLICATION_PACKAGE_PREFIX = "com.hrr.backend.";

	private SafeExceptionSummary() {
	}

	/** 예외 메시지를 제외하고 서버 진단에 필요한 타입과 애플리케이션 발생 위치만 반환한다. */
	public static String summarize(Throwable throwable) {
		if (throwable == null) {
			return "type=none,rootCause=none,origin=unknown";
		}

		Throwable rootCause = findRootCause(throwable);
		StackTraceElement origin = findApplicationFrame(rootCause);
		if (origin == null && rootCause != throwable) {
			origin = findApplicationFrame(throwable);
		}

		return "type=" + throwable.getClass().getSimpleName()
				+ ",rootCause=" + rootCause.getClass().getSimpleName()
				+ ",origin=" + formatOrigin(origin);
	}

	private static Throwable findRootCause(Throwable throwable) {
		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		Throwable current = throwable;
		visited.add(current);

		while (current.getCause() != null && visited.add(current.getCause())) {
			current = current.getCause();
		}
		return current;
	}

	private static StackTraceElement findApplicationFrame(Throwable throwable) {
		for (StackTraceElement frame : throwable.getStackTrace()) {
			if (frame.getClassName().startsWith(APPLICATION_PACKAGE_PREFIX)) {
				return frame;
			}
		}
		return null;
	}

	private static String formatOrigin(StackTraceElement origin) {
		if (origin == null) {
			return "unknown";
		}

		String className = origin.getClassName();
		int lastDot = className.lastIndexOf('.');
		String simpleClassName = lastDot >= 0 ? className.substring(lastDot + 1) : className;
		return simpleClassName + "." + origin.getMethodName() + ":" + origin.getLineNumber();
	}
}
