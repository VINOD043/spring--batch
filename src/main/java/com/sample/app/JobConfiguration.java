package com.sample.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.jms.JmsItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.sample.app.model.User;

@Configuration
@EnableBatchProcessing
public class JobConfiguration{
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private DataSource dataSource;
	
	@Autowired
	JmsTemplate jmsTemplate;
	
	private final String SQL_QUERY = "SELECT ID, FIRSTNAME, LASTNAME,"
			+ "EMAIL, GENDER FROM springbatchdb.USER";

	//@Bean
	public JdbcCursorItemReader<User> jdbcCursorItemReader() throws InterruptedException {
		JdbcCursorItemReader<User> reader = new JdbcCursorItemReader<User>();
		reader.setDataSource(dataSource);
		reader.setSql(SQL_QUERY);
		reader.setFetchSize(50);
		reader.setRowMapper(new UserRowMapper());
		System.out.println("Sleeping for 5 minutes");
		Thread.sleep(60000);
		
		return reader;
	}

	//@Bean
	public FlatFileItemWriter<User> jsonFileItemWriter() throws Exception {
		FlatFileItemWriter<User> flatFileItemWriter = new FlatFileItemWriter<>();
		
		//JmsItemWriter<User> jmsItemWriter = new JmsItemWriter<User>();
		//jmsItemWriter.setJmsTemplate(jmsTemplate);

		flatFileItemWriter.setLineAggregator(new UserJsonLineAggregator());
		//String outFilePath = "/Users/Shared/result.json";
		String dateTime = getDateTime();
		
		String outFilePath = "result"+dateTime.replaceAll(":", ".")+".json";

		flatFileItemWriter.setResource(new FileSystemResource(outFilePath));
		flatFileItemWriter.open(new ExecutionContext());
		flatFileItemWriter.close();

		flatFileItemWriter.afterPropertiesSet();
		System.out.println("----------jsonFileItemWriter ----------------");
		return flatFileItemWriter;
	}
	
	//@Bean
	public JmsItemWriter<User> jsonFileItemWriterToMQ() throws Exception {
		System.out.println("jsonFileItemWriterToMQ sleep for 1 minute");
		Thread.sleep(60000);
		JmsItemWriter<User> jmsItemWriter = new JmsItemWriter<User>();
		jmsItemWriter.setJmsTemplate(jmsTemplate);
		//jmsTemplate.convertAndSend(new User());
		return jmsItemWriter;
	}
	
	private String getDateTime() {
		LocalDateTime dateTime = LocalDateTime.now();
		return dateTime.toString();
	}

	//@Bean
	public CompositeItemWriter<User> compositeItemWriter() throws Exception {
		CompositeItemWriter<User> compositeItemWriter = new CompositeItemWriter<>();

		List<ItemWriter<? super User>> itemWriters = new ArrayList<>();
		System.out.println("**********compositeItemWriter***********"+itemWriters.size());
		itemWriters.add(jsonFileItemWriterToMQ());

		compositeItemWriter.setDelegates(itemWriters);
		compositeItemWriter.afterPropertiesSet();

		return compositeItemWriter;
	}

	//@Bean
	public Step step1() throws Exception {
		return this.stepBuilderFactory
				.get("step1")
				.allowStartIfComplete(true)
				.startLimit(100)
				.<User, User>chunk(10)
				.reader(jdbcCursorItemReader())
				.writer(compositeItemWriter())
				.build();
	}

	@Bean
	public Job myJob(JobRepository jobRepository,
			PlatformTransactionManager platformTransactionManager)
			throws Exception {

		return jobBuilderFactory
				.get("My-First-Job")
				.start(step1())
				.build();
	}
	
	
}
