package org.sample.telegrambot;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Hello world!
 *
 */
@QuarkusMain
public class App implements QuarkusApplication
{
	@Override
	public int run(String... args) throws Exception {
		Quarkus.waitForExit();
		return 0;
	}
}
