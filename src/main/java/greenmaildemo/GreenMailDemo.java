package greenmaildemo;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.util.Properties;


@SuppressWarnings("CallToPrintStackTrace")
public class GreenMailDemo
{
    static GreenMail greenMail;
    static String PW = "pwd1";
    static String USER = "gmtestuser";
    static final String HOST = "localhost";
    static final String EMAIL_USER_ADDRESS = USER + "@localhost";

    public static void main(String[] args)
    {
        try {
            // Initialize GreenMail server
            greenMail = new GreenMail(ServerSetupTest.SMTP_POP3_IMAP)
                    .withConfiguration(GreenMailConfiguration.aConfig()
                            .withUser(EMAIL_USER_ADDRESS, USER, PW));

            greenMail.start();

            sendMail("Test1");
            sendMail("Test2");
            sendMailWithSmtp("Test3");
            sendMailWithSmtp("Test4");

            checkMailByObject();
            checkMailByImap();
            checkMailByPop3();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        greenMail.stop();
    }

    static void checkMailByObject()
    {
        System.out.println("================== API ================= ");

        // Retrieve received messages
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        for (MimeMessage receivedMessage : receivedMessages) {
            try {
                System.out.println("Subject: " + receivedMessage.getSubject());
                System.out.println("Content: " + receivedMessage.getContent());
            }
            catch (MessagingException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void checkMailByImap()
    {
        // see also https://www.hascode.com/2012/07/integration-testing-imap-smtp-and-pop3-with-greenmail/

        // Configure properties for connecting to GreenMail IMAP server
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.host", HOST); // GreenMail server host
        props.setProperty("mail.imap.port", "3143"); // GreenMail IMAP port

        try {
            Session session = Session.getInstance(props, null);

            // Connect to GreenMail IMAP server
            Store store = session.getStore("imap");
            store.connect(USER, PW);
            if (store.isConnected()) {

                System.out.println("================== IMAP ================= ");

                // List available folders
                Folder[] folders = store.getDefaultFolder().list("*");
                System.out.print("Available folders: ");
                for (Folder folder : folders) {
                    System.out.print(folder.getName() + ", ");
                }
                System.out.println();

                // List inbox messages
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                Message[] messages = inbox.getMessages();
                System.out.flush();
                for (Message message : messages) {
                    System.out.println("Subject: " + message.getSubject());
                }

                inbox.close(false);
                store.close();
            } else {
                throw new IllegalStateException("Store not connected.");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public static void checkMailByPop3() throws MessagingException
    {
        System.out.println("================== POP3 ================= ");

        Properties props = new Properties();
        Session session = Session.getInstance(props);
        URLName urlName = new URLName("pop3", HOST, ServerSetupTest.POP3.getPort(), null, USER, PW);
        Store store;
        try {
            store = session.getStore(urlName);
            store.connect();

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            Message[] messages = inbox.getMessages();
            for (Message m : messages) {
                System.out.println("Subject: " + m.getSubject());
            }
            inbox.close(false);
        }
        catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
        store.close();
    }

    static void sendMail(String subject)
    {
        // Create properties
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", HOST);
        props.setProperty("mail.smtp.port", "3025");

        // Create session
        Session session = Session.getInstance(props, null);

        try {
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_USER_ADDRESS));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("recipient@example.com"));
            message.setSubject(subject);
            message.setText("This is a test content.");

            // send
            GreenMailUser user = greenMail.getUserManager().getUser(USER);
            user.deliver(message);
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    static void sendMailWithSmtp(String subject)
    {
        // Create properties for SMTP
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", HOST);
        props.setProperty("mail.smtp.port", "3025");

        // Create session
        Session session = Session.getInstance(props, null);

        try {
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_USER_ADDRESS));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_USER_ADDRESS));
            message.setSubject(subject);
            message.setText("This is a test content.");

            // send with SMTP
            Transport.send(message);
            System.out.println("Email Message Sent Successfully");
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
