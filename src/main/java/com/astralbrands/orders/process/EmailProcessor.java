package com.astralbrands.orders.process;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.annotation.PostConstruct;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;

@Component
public class EmailProcessor implements Processor, AppConstants {

	Logger log = LoggerFactory.getLogger(EmailProcessor.class);
	private static final String SUBJECT = "INTERNATIONAL BRAND ORDER FORM";
	private static final String SUBJECT_2 = "40 BOXES ORDER FORM";
	private static final String SUBJECT_3 = "COSMEDIX INT. BRAND ORDER FORM";
	private static final String SUBJECT_4 = "PURCOSMETICS INT. BRAND ORDER FORM";
	private static final String SUBJECT_5 = "BUTTER-LONDON INT. BRAND ORDER FORM";
	private static final String SUBJECT_6 = "NYKKA INT. ORDER FORM";

	// SUBJECT LINES FOR LOCAL ORDER FORMS FOR ASHLEY
	private static final String SUBJECT_7 = "COSMEDIX ORDER FORM";
	private static final String SUBJECT_8 = "PURCOSMETICS ORDER FORM";
	private static final String SUBJECT_9 = "BUTTER-LONDON ORDER FORM";
	private static final String SUBJECT_10 = "ALOETTE ORDER FORM";

	public String emailFlag = "";

	@Autowired
	ReadXslxFileProcessor readXslxFileProcessor;

	@Autowired
	BoxesExportProcess boxesExportProcess;

	@Value("${smtp.host}")
	private String host;
	@Value("${smtp.port}")
	private String port;
	@Value("${smtp.username}")
	private String userName;

	@Value("${smtp.password}")
	private String password;
	@Value("${smtp.from}")
	private String from;
	@Value("${smtp.to}")
	private String toList;

	JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

	@PostConstruct
	public void init() {
		javaMailSender.setHost(host);
		javaMailSender.setPort(587);
		javaMailSender.setUsername(userName);
		javaMailSender.setPassword(password);

		Properties props = javaMailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.required", "true");
		props.put("mail.smtpClient.EnableSsl", "false");
		props.put("mail.debug", "true");
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		String csvFileData = exchange.getProperty(CSV_DATA, String.class);
//		String site = exchange.getProperty(SITE_NAME, String.class);
		String iFile = exchange.getProperty("IFILE", String.class);
		String realFileName = exchange.getProperty(INPUT_FILE_NAME, String.class);

		sendEmail(csvFileData, iFile, realFileName, exchange);
//		if(fileName[0].equalsIgnoreCase("COS") || fileName[0].equalsIgnoreCase("BUT") || fileName[0].equalsIgnoreCase("PUR")
//				|| fileName[0].equalsIgnoreCase("LYS") || fileName[0].equalsIgnoreCase("NYKKA") || fileName[0].equalsIgnoreCase("BOX"))
//		{
//			brand = exchange.getProperty(SITE_NAME, String.class);
//			exchange.getMessage().setBody(csvFileData);
//			exchange.getMessage().setBody(iFile);
//			exchange.getMessage().setBody(brand);
//			exchange.getMessage().setBody(fileName);
//			sendEmail(csvFileData, iFile, brand, fileName, exchange);
//		} else if (fileName[2].equalsIgnoreCase("COS") || fileName[2].equalsIgnoreCase("BUT") || fileName[2].equalsIgnoreCase("PUR")
//				|| fileName[2].equalsIgnoreCase("LYS") || fileName[2].equalsIgnoreCase("HUB") || fileName[2].equalsIgnoreCase("TRACK")
//				|| fileName[2].equalsIgnoreCase("US") || fileName[2].equalsIgnoreCase("CA"))
//		{
//			brandTwo = exchange.getProperty(SITE_TWO, String.class);
//			exchange.getMessage().setBody(csvFileData);
//			exchange.getMessage().setBody(iFile);
//			exchange.getMessage().setBody(brandTwo);
//			exchange.getMessage().setBody(fileName);
//			sendEmail(csvFileData, iFile, brandTwo, fileName, exchange);
//		}
//		switch(fileName[0]) {
//			case "COS", "PUR", "BUT", "LYS", "NYKKA", "BOX" -> {
//				brand = exchange.getProperty(SITE_NAME, String.class);
//				exchange.getMessage().setBody(csvFileData);
//				exchange.getMessage().setBody(iFile);
//				exchange.getMessage().setBody(brand);
//				exchange.getMessage().setBody(fileName);
//				sendEmail(csvFileData, iFile, brand, fileName);
//			}
//		}
//		switch(fileName[2]) {
//			case "COS", "BUT", "HUB", "PUR", "US", "CA", "TRACK", "LTRACK", "LYS" -> {

//			}
//		}

//		if(fileName[0].equalsIgnoreCase("COS")) {
//			brandTwo = exchange.getProperty(SITE_TWO, String.class);
//			exchange.getMessage().setBody(csvFileData);
//			exchange.getMessage().setBody(iFile);
//			exchange.getMessage().setBody(brandTwo);
//			exchange.getMessage().setBody(fileName);
//			sendEmail(csvFileData, iFile, brandTwo, fileName);
//		} else {
//			brand = exchange.getProperty(SITE_NAME, String.class);
//			exchange.getMessage().setBody(csvFileData);
//			exchange.getMessage().setBody(iFile);
//			exchange.getMessage().setBody(brand);
//			exchange.getMessage().setBody(fileName);
//			sendEmail(csvFileData, iFile, brand, fileName);
//		}
	}

	public void sendEmailTracking(String subject, String iFile, String site, String fileName) {

		log.info("sending .........");

		JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
		javaMailSender.setHost(host);
		javaMailSender.setPort(587);

		javaMailSender.setUsername(userName);
		javaMailSender.setPassword(password);

		Properties props = javaMailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.required", "true");
		props.put("mail.smtpClient.EnableSsl", "false");
		props.put("mail.debug", "true");

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			// helper = new MimeMessageHelper(mimeMessage, true);
			if (toList != null && toList.length() > 0 && toList.contains(SEMI_COMMA)) {
				String[] toAdd = toList.split(SEMI_COMMA);
				for (String to : toAdd) {
					mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(to));
				}
			} else {
				mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(toList));
			}

			mimeMessage.setFrom(from);
			mimeMessage.setSubject(subject);
			/*
			 * message.setContent(csvFile, "text/html"); message.setFileName("htmlFile");
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Multipart multiPart = new MimeMultipart();
			MimeBodyPart attachFilePart = new MimeBodyPart();
//			attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
//			attachFilePart.setFileName(fileName + DOT_CSV);


			MimeBodyPart iFileBody = new MimeBodyPart();
			iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
			iFileBody.setFileName(fileName+ DOT_TXT);
			multiPart.addBodyPart(iFileBody);

			String body = "<html> Hi Team, <br>" + "<br><b style='color:black;'>FLIGHT TRACKING FILE :</b><br>"
					+ "<br><i style='color:black;'>"+ iFileBody.getFileName().toString()+".txt</i><br><br>"
					+ "<br> Regards,<br>" + "<br>Jacob Schirm </html>";
			BodyPart msgBody = new MimeBodyPart();
			msgBody.setText(body);

//			multiPart.addBodyPart(attachFilePart);
			multiPart.addBodyPart(msgBody);
			mimeMessage.setContent(multiPart);

		} catch (Exception e) {
			e.printStackTrace();
		}

		javaMailSender.send(mimeMessage);
	}

	public void sendEmail(String csvFile, String iFile, String fileName, Exchange exchange) {  //----String site

		log.info("sending .........");

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessage mm1 = javaMailSender.createMimeMessage();
		MimeMessage mm2 = javaMailSender.createMimeMessage();
		MimeMessage mm3 = javaMailSender.createMimeMessage();


		String name = exchange.getProperty(INPUT_FILE_NAME, String.class);
		String[] nameArray = name.split("_");
		String custNumb = nameArray[2];
		int number = 0;
		if (!custNumb.isEmpty() && custNumb != null && custNumb.matches("([0-9])")) {
			number = Integer.parseInt(custNumb);
		}
		//----int e = Integer.parseInt(custNumb);-----USE IF ABOVE 'if' BLOCK NOT WORK

		try {
			// helper = new MimeMessageHelper(mimeMessage, true);

			if (toList != null && toList.length() > 0 && toList.contains(SEMI_COMMA)) {
				String[] emails = toList.split(SEMI_COMMA);
				String firstEmail = emails[0];  // ----Jacob's Email
				String secondEmail = emails[1]; // ----Ruth's Email
				String thirdEmail = emails[2];  // ----Grace's Email
				String fourthEmail = emails[3]; // ----denise's - darmstrong@... Email

				BodyPart msgBody = new MimeBodyPart();
				Multipart multiPart = new MimeMultipart();
				MimeBodyPart attachFilePart = new MimeBodyPart();

				String[] newFileName = fileName.split("_");

				MimeBodyPart iFileBody = new MimeBodyPart();
				String bodyText = "";
				String subject = "";
				String flag = newFileName[2].toString();
				String flagTwo = newFileName[0].toString();

//				flagTwo = fileName.substring(fileName.lastIndexOf(UNDERSCORE) + 1);
				if(flagTwo.equalsIgnoreCase("COS") || flagTwo.equalsIgnoreCase("BUT") || flagTwo.equalsIgnoreCase("PUR")) {

                    if(flagTwo.equalsIgnoreCase("COS")) {
						subject = SUBJECT_3;
						String cosmedixMsgBody = "<html>Hi Team, <br>" + "<br><b style='color:black;'>COSMEDIX INTERNATIONAL ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setText(cosmedixMsgBody);
						msgBody.setContent(cosmedixMsgBody, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(secondEmail));// ----Ruth's Email
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mimeMessage.setFrom(from);
						mimeMessage.setSubject(subject);
						mimeMessage.setContent(multiPart, "text/html");
						javaMailSender.send(mimeMessage);

						System.out.println("EMAIL SENDING......" + subject);
					}
					if(flagTwo.equalsIgnoreCase("PUR")) {
						subject = SUBJECT_4;
						String purCosmeticsMsgBody = "<html> Hi Team, <br>" + "<br><b style='color:black;'>PURCOSMETICS INTERNATIONAL ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setContent(purCosmeticsMsgBody, "text/html");
						msgBody.setText(purCosmeticsMsgBody);
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(secondEmail));// ----Ruth's Email
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mimeMessage.setFrom(from);
						mimeMessage.setSubject(subject);
						mimeMessage.setContent(multiPart, "text/html");
						javaMailSender.send(mimeMessage);

						System.out.println("Email sending......" + subject);
					}
					if(flagTwo.equalsIgnoreCase("BUT")) {
						subject = SUBJECT_5;
						String butterLondonMsgBody = "<html> Hi Team, <br>" + "<br><b style='color:black;'>BUTTER-LONDON INTRNATIONAL ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setText(butterLondonMsgBody);
						msgBody.setContent(butterLondonMsgBody, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(secondEmail));// ----Ruth's Email
						mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mimeMessage.setFrom(from);
						mimeMessage.setSubject(subject);
						mimeMessage.setContent(multiPart, "text/html");
						javaMailSender.send(mimeMessage);

						System.out.println("Email sending......" + subject);
					}
				}
				else if (flagTwo.equalsIgnoreCase("BOX") || flagTwo.equalsIgnoreCase("NYKKA")) {

					mm2.setFrom(from);
//					String newEmail = "";
//					if(!this.emailFlag.isEmpty()) {
//						newEmail = this.emailFlag;
//					}



					if(flagTwo.equalsIgnoreCase("BOX")) {
						subject = SUBJECT_2;
						String email = "";
						String j = exchange.getProperty(CUST_NUMBER, String.class);
						int i = Integer.parseInt(j);
						if(i < 140000 && i > 100000) {
							email = "darmstrong@astralbrands.com";
						}
						else if (i > 140000) {
							email = "gallen@astralbrands.com";
						}

						String boxes = "<html> Hi Team, <br>" + "<br><b style='color:black;'>40 BOXES ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setText(boxes);
						msgBody.setContent(boxes, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);

//						if(!newEmail.isEmpty()) {
//							mm2.addRecipient(RecipientType.TO, new InternetAddress("jschirm@astralbrands.com"));
//							mm2.addRecipient(RecipientType.TO, new InternetAddress(newEmail));
//						} else {
//							mm2.addRecipient(RecipientType.TO, new InternetAddress("jschirm@astralbrands.com"));
//						}
						mm2.addRecipient(RecipientType.TO, new InternetAddress("jschirm@astralbrands.com"));// ----Jacob's Email
						//mm2.addRecipient(RecipientType.TO, new InternetAddress(secondEmail));// ----Ruth's Email
						mm2.addRecipient(RecipientType.TO, new InternetAddress(email)); // ----Grace's Email
						mm2.setFrom(from);
						mm2.setSubject(subject);
						mm2.setContent(multiPart, "text/html");
						javaMailSender.send(mm2);

					}
					if(flagTwo.equalsIgnoreCase("NYKKA")) {
						subject = SUBJECT_6;
						String nykkaMsgBody = "<html> Hi Team, <br>" + "<br><b style='color:black;'>NYKKA INTERNATIONAL ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
//						msgBody.setText(nykkaMsgBody);
						msgBody.setContent(nykkaMsgBody, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);

//						if(!newEmail.isEmpty()) {
//							mm2.addRecipient(RecipientType.TO, new InternetAddress("jschirm@astralbrands.com"));
//							mm2.addRecipient(RecipientType.TO, new InternetAddress(newEmail));
//						} else {
//							mm2.addRecipient(RecipientType.TO, new InternetAddress("jschirm@astralbrands.com"));
//						}
						mm2.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mm2.addRecipient(RecipientType.TO, new InternetAddress(secondEmail));// ----Ruth's Email
						mm2.setFrom(from);
						mm2.setSubject(subject);
						mm2.setContent(multiPart);
						javaMailSender.send(mm2);

						System.out.println("Email sending......" + subject);
					}
				} else {																						//if (flagTwo.equalsIgnoreCase("COS") || flagTwo.equalsIgnoreCase("BUT") || flagTwo.equalsIgnoreCase("PUR")
																												//|| flagTwo.equalsIgnoreCase("US") || flagTwo.equalsIgnoreCase("CA"))
					if(flag.equalsIgnoreCase("COS")) {
						subject = SUBJECT_7;
						bodyText = "<html> Hi Team, <br>" + "<br><b style='color:black;'>COSMEDIX ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setText(bodyText);
						msgBody.setContent(bodyText, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mm3.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mm3.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mm3.setFrom(from);
						mm3.setSubject(subject);
						mm3.setContent(multiPart);
						javaMailSender.send(mm3);

						System.out.println("Email sending......" + subject);
					}
					if(flag.equalsIgnoreCase("PUR")) {
						subject = SUBJECT_8;
						bodyText = "<html> Hi Team, <br>" + "<br><b style='color:black;'>PURCOSMETICS ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";

						msgBody.setText(bodyText);
						msgBody.setContent(bodyText, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mm3.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mm3.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mm3.setFrom(from);
						mm3.setSubject(subject);
						mm3.setContent(multiPart);
						javaMailSender.send(mm3);

						System.out.println("Email sending......" + subject);

					}
					if(flag.equalsIgnoreCase("BUT")) {
						subject = SUBJECT_9;
						bodyText = "<html> Hi Team, <br>" + "<br><b style='color:black;'>BUTTER-LONDON ORDER FORM i-files :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setText(bodyText);
						msgBody.setContent(bodyText, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mm3.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mm3.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mm3.setFrom(from);
						mm3.setSubject(subject);
						mm3.setContent(multiPart);
						javaMailSender.send(mm3);

						System.out.println("Email sending......" + subject);
					}
					if(flag.equalsIgnoreCase("US") || flag.equalsIgnoreCase("CA")) {
						subject = SUBJECT_10;
						bodyText = "<html> Hi Team, <br>" + "<br><b style='color:black;'>ALOETTE ORDER FORM i-file :</b><br>"
								+ "<br><i style='color:black;'>"+ name +".txt</i><br>" + "\n\r"
								+ "<br><i style='color:black;'>"+ name +".csv</i><br>" + "\n\r" + " \n\r<br> Regards,<br>\n\r" + "<br>Jacob Schirm </html>";
						msgBody.setText(bodyText);
						msgBody.setContent(bodyText, "text/html");
						attachFilePart.setDataHandler(new DataHandler(new ByteArrayDataSource(csvFile.getBytes(), "text/csv")));
						attachFilePart.setFileName(name + DOT_CSV);
						iFileBody.setDataHandler(new DataHandler(new ByteArrayDataSource(iFile.getBytes(), "text/plain")));
						iFileBody.setFileName(name + DOT_TXT);
						multiPart.addBodyPart(iFileBody);
						multiPart.addBodyPart(attachFilePart);
						multiPart.addBodyPart(msgBody);
						mm3.addRecipient(RecipientType.TO, new InternetAddress(firstEmail));// ----Jacob's Email
						mm3.addRecipient(RecipientType.TO, new InternetAddress(fourthEmail));// ----Ashley's Email
						mm3.setFrom(from);
						mm3.setSubject(subject);
						mm3.setContent(multiPart, "text/html");


						System.out.println("Email sending......" + subject);
					}

//					mimeMessage.setSubject(subject);
//
//					mimeMessage.setContent(multiPart);
//					javaMailSender.send(mimeMessage);
//					System.out.println("Email sending......" + subject);
				}

			}
			/*
			 * message.setContent(csvFile, "text/html"); message.setFileName("htmlFile");
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getFormEmail(String cell) {
		if(cell.equalsIgnoreCase("BGMA")) {
			this.emailFlag = "gallen@astralbrands.com";
		} else if (cell.equalsIgnoreCase("PGMA")) {
			this.emailFlag = "darmstrong@astralbrands.com";
		}
		return this.emailFlag;
	}

	private String getCountry(String site) {
		if (US_STR.equals(site)) {
			return "USA";
		} else {
			return "CANADA";
		}
	}

}
