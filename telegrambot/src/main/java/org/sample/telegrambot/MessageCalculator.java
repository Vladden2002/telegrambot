package org.sample.telegrambot;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("message")
public class MessageCalculator {
	
	@Inject
	TelegramBot bot;
	
	@GET
	public void message() {
		String message = "Now it is " + LocalTime.now() + ": " + UUID.randomUUID();
		bot.sendMessage(message);
	}
}
