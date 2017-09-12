package org.gotti.wurmunlimited.mods.vyspermod.ui;

import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.builder;
import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.button;
import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.checkbox;
import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.harray;
import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.input;
import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.passthough;
import static org.gotti.wurmunlimited.modsupport.bml.BmlBuilder.text;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.mods.vyspermod.util.VysperProperties;
import org.gotti.wurmunlimited.modsupport.bml.BmlBuilder;
import org.gotti.wurmunlimited.modsupport.bml.BmlNodeBuilder;
import org.gotti.wurmunlimited.modsupport.bml.TextStyle;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;

import com.wurmonline.server.Message;
import com.wurmonline.server.questions.Question;

public class ManageVysperQuestion implements ModQuestion {

	private static final Logger LOGGER = Logger.getLogger(ManageVysperQuestion.class.getName());
	
	private static final String ANSWER_ENABLE_VYSPER = "enablevysper";
	private static final String ANSWER_NEW_PASSWORD = "newpassword";
	
	private static final String MESSAGE_CHANNEL = ":Event";
	
	private final long playerId;
	private final boolean vysperEnabled;
	private final boolean vysperPasswordSet;
	
	public ManageVysperQuestion(long playerId) {
		this.playerId = playerId;
		this.vysperEnabled = VysperProperties.isVysperEnabled(playerId);
		this.vysperPasswordSet = VysperProperties.isVysperPasswordSet(playerId);
	}
	
	@Override
	public void answer(Question question, Properties answers) {
		LOGGER.info(answers.toString());
		
		final String newPassword = answers.getProperty(ANSWER_NEW_PASSWORD, "");
		final boolean enableVysper = Boolean.parseBoolean(answers.getProperty(ANSWER_ENABLE_VYSPER));
		
		if (enableVysper) {
			if (!vysperPasswordSet && newPassword.isEmpty()) {
				question.getResponder().getCommunicator().sendMessage(new Message(question.getResponder(), Message.SERVERALERT, MESSAGE_CHANNEL, "Set a password to enable Vysper"));
				return;
			}
			
			VysperProperties.setVysperEnabled(playerId, true);
			question.getResponder().getCommunicator().sendMessage(new Message(question.getResponder(), Message.SERVERNORMAL, MESSAGE_CHANNEL, "Vysper enabled"));
		} else {
			VysperProperties.setVysperEnabled(playerId, false);
			question.getResponder().getCommunicator().sendMessage(new Message(question.getResponder(), Message.SERVERNORMAL, MESSAGE_CHANNEL, "Vysper disabled"));
		}
		
		if (!newPassword.isEmpty()) {
			try {
				VysperProperties.setPlayerPasswordHash(playerId, newPassword);
				question.getResponder().getCommunicator().sendMessage(new Message(question.getResponder(), Message.SERVERNORMAL, MESSAGE_CHANNEL, "Vysper password changed"));
			} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
				question.getResponder().getCommunicator().sendMessage(new Message(question.getResponder(), Message.SERVERALERT, MESSAGE_CHANNEL, "Failed to set password"));
			}
		}
	}

	@Override
	public void sendQuestion(Question question) {
		
		final BmlNodeBuilder changePasswordText;
		if (this.vysperPasswordSet) {
			changePasswordText = text("Change password", TextStyle.BOLD);
		} else {
			changePasswordText = text("Password is required to enable Vysper", TextStyle.BOLD).red();
		}
		
		final BmlBuilder content = builder()
				.withNode(passthough("id", String.valueOf(question.getId())))
				.withNode(text("Configure", TextStyle.BOLD))
				.withNode(checkbox(ANSWER_ENABLE_VYSPER, "Enable Vysper account").withAttribute("selected", vysperEnabled))
				.withNode(text(""))
				.withNode(changePasswordText)
				.withNode(input(ANSWER_NEW_PASSWORD))
				.withNode(harray().withNode(button("submit", "Set options")));
		
		
		String bml = content.wrapAsDialog(question.getTitle(), false, true, true).buildBml();
		LOGGER.info(bml.toString());
		sendBml(question, 300, 300, true, true, bml);
	}
	
	

}
