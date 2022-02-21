package com.sample.app;


import org.springframework.batch.item.file.transform.LineAggregator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sample.app.model.User;

public class UserJsonLineAggregator implements LineAggregator<User>{
	private ObjectMapper objectMapper = new ObjectMapper();

	{
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@Override
	public String aggregate(User user) {

		try {
			return objectMapper.writeValueAsString(user);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error Occured while serializing Employee instance : " + user);
		}
	}
	
}
