package edu.rit.se441.project2.actors;

import java.util.Random;

import edu.rit.se441.project2.messages.BodyCheckReport;
import edu.rit.se441.project2.messages.BodyCheckRequestsNext;
import edu.rit.se441.project2.messages.CanISendYouAPassenger;
import edu.rit.se441.project2.messages.EndOfDay;
import edu.rit.se441.project2.messages.GoToBodyCheck;
import edu.rit.se441.project2.messages.Initialize;
import edu.rit.se441.project2.messages.Register;
import edu.rit.se441.project2.nonactors.Consts;
import edu.rit.se441.project2.nonactors.Logger;
import edu.rit.se441.project2.nonactors.Passenger;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class BodyCheckActor extends UntypedActor {
	private static final Logger logger = new Logger(BodyCheckActor.class);
	
	// child of scanners is the security
	private static final String MY_CHLDRN = Consts.NAME_ACTORS_SECURITY.value();
	// parent is the line
	private static final String MY_PARENT = Consts.NAME_ACTORS_LINE.value();
	
	private final int lineNumber;
	private ActorRef myLine = null; // set after the init message
	private ActorRef mySecurity = null; // set after the init message
	private boolean isAcceptingPassengers = true; // set and reset during the
													// simulation

	public BodyCheckActor(final int lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
		Consts msgReceived = Consts.DEBUG_MSG_RECEIVED;

		// initialization message
		if (arg0 instanceof Initialize) {
			logger.debug(msgReceived, Consts.NAME_MESSAGES_INIT, MY_CHLDRN);

			if (childrenAreRegistered()) {
				logger.error(Consts.DEBUG_MSG_CHLD_ALR_REG, MY_CHLDRN);
				// TODO what should we do here?
				// returning is fine
				return;
			}

			Initialize init = (Initialize) arg0;
			mySecurity = init.getSecurityActor(this.lineNumber);
			myLine = init.getLineActor(lineNumber);

			logger.debug(Consts.DEBUG_MSG_REG_MY_CHILD, MY_PARENT);
			myLine.tell(new Register(1));
		}

		/*
		 * Message From Line
		 */
		if (arg0 instanceof GoToBodyCheck) {
			if (!childrenAreRegistered()) {
				logger.error(Consts.ERROR_MSG_CHLD_NOT_REG, MY_CHLDRN);				
				// TODO what should we do here?
				// returning is fine
				return;
			}
			
			logger.debug(msgReceived, Consts.NAME_MESSAGES_GO_TO_BODY_CHECK,
					MY_CHLDRN);
			
			this.isAcceptingPassengers = false;

			// critical problem area below
			GoToBodyCheck GoToBodyCheck = (GoToBodyCheck) arg0;
			performBodyCheck(GoToBodyCheck.getPassenger());
			// end critical problem area
		}

		/*
		 * Message From Line
		 */
		if (arg0 instanceof CanISendYouAPassenger) {
			logger.debug(msgReceived,
					Consts.NAME_MESSAGES_CAN_I_SEND_YOU_A_PASSENG, MY_CHLDRN);
			if (this.isAcceptingPassengers) {
				CanISendYouAPassenger cISYAP = (CanISendYouAPassenger) arg0;
				ActorRef myLine = cISYAP.getLineActor();
				
				logger.debug(Consts.NAME_MESSAGES_BODY_CHECK_REQUESTS_NEXT, MY_PARENT);
				myLine.tell(new BodyCheckRequestsNext());
			} else {
				// swallow the message
			}
		}

		if (arg0 instanceof EndOfDay) {
			logger.debug(msgReceived, Consts.NAME_MESSAGES_END_OF_DAY,
					MY_CHLDRN);
			// TODO what to do here?
			shutDown();

		}
	}

	private void shutDown() {
		// TODO Auto-generated method stub

	}

	// Function takes a passenger and messages Security whether it passes or
	// fails
	// BodyCheckActor is accepting passengers after this message
	private void performBodyCheck(Passenger p) {
		double n = Math.random();
		BodyCheckReport myBodyReport;
		
		Consts bodyChkRptLbl = Consts.NAME_MESSAGES_BODY_CHECK_REPORT;
		Consts securityLbl = Consts.NAME_ACTORS_SECURITY;
		Consts bodyChkLbl = Consts.NAME_ACTORS_BODY_CHECK;
		
		if (n > .5) {
			// Passes scan
			myBodyReport = new BodyCheckReport(p, true);
		} else {
			// Fails scan
			myBodyReport = new BodyCheckReport(p, false);
		}
		
		logger.debug(Consts.DEBUG_MSG_SEND_OBJ_TO_IN_MESS, bodyChkRptLbl, securityLbl, bodyChkLbl);
		this.mySecurity.tell(myBodyReport);
		this.isAcceptingPassengers = true;

	}

	/**
	 * This may be worth to do before something like a Passenger is sent!
	 */
	private boolean childrenAreRegistered() {
		return (mySecurity != null);
	}

}
