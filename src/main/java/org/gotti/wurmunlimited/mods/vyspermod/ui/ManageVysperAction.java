package org.gotti.wurmunlimited.mods.vyspermod.ui;

import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ActionPropagation;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class ManageVysperAction implements ActionPerformer {
	
	private final short actionId;
	private final ActionEntry actionEntry;

	public ManageVysperAction() {
		this.actionId = (short) ModActions.getNextActionId();
		// Create the action entry
		this.actionEntry = new ActionEntryBuilder(actionId, "Manage Vysper", "managing", new int[] { 0 /* ACTION_TYPE_QUICK */, 37 /* ACTION_TYPE_NEVER_USE_ACTIVE_ITEM */}).build();
		// Register the action entry
		ModActions.registerAction(actionEntry);
	}
	
	private boolean action(Action action, Creature performer) {
		ModQuestions.createQuestion(performer, "Manage Vysper", "Manage Vysper", -10, new ManageVysperQuestion(performer.getWurmId())).sendQuestion();
		return propagate(action, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.FINISH_ACTION);
	}
	
	public boolean action(Action action, Creature performer, int menuId, short num, float counter) {
		return action(action, performer);
	}
	
	public boolean action(Action action, Creature performer, Item target, short num, float counter) {
		return action(action, performer);
	}
	
	
	@Override
	public short getActionId() {
		return actionId;
	}
	
	@Override
	public boolean defaultPropagation(Action action) {
		return ActionPerformer.super.defaultPropagation(action);
	}
}
