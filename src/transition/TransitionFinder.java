package siteswapsuite;

public class TransitionFinder {

	int numHands;
	private State from;
	private State to;

	public TransitionFinder(State from, State to) throws ImpossibleTransitionException {
		if(!from.isFinite() || !to.isFinite()) {
			throw new ImpossibleTransitionException();
		}
		// make copies of the states, so as not to muck up the originals
		this.numHands = from.numHands();
		this.from = from.deepCopy();
		this.to = to.deepCopy();

		// equalize the state lengths
		if(this.from.finiteLength() < this.to.finiteLength()) {
			this.from.getFiniteNode(this.to.finiteLength() - 1);
		} else if (this.from.finiteLength() > this.to.finiteLength()) {
			this.to.getFiniteNode(this.from.finiteLength() - 1);
		}

	}

	public TransitionResults findTransitions(TransitionOptions options) {
		GeneralizedTransition generalizedTransition;
		// see if either state is empty
		if(from.finiteLength() == 0 || to.finiteLength() == 0) {
			generalizedTransition = new GeneralizedTransition(from.numHands());
		} else {
			if(!options.allowExtraSqueezeCatches && !options.generateBallAntiballPairs) {
				generalizedTransition = this.findStandardTransition(options.minTransitionLength);
			} else {
				Util.printf("ERROR: non-standard transition options not yet supported", Util.DebugLevel.ERROR);
				System.exit(1);
				return null;
			}
		}
		return new TransitionResults(generalizedTransition, options.maxTransitions, options.selectTransition);
	}

	private GeneralizedTransition findStandardTransition(int minLength) {
		GeneralizedTransition transition = new GeneralizedTransition(this.numHands);

		int b = 0; // index of beat in output siteswap

		Util.printf("s1: " + this.from.toString(), Util.DebugLevel.DEBUG);
		Util.printf("s2: " + this.to.toString(), Util.DebugLevel.DEBUG);

		State.DiffSum diffs;
		int futureCatches = 0;
		int futureAnticatches = 0;

		diffs = this.from.diffSums(this.to); // compute difference sum
		Util.printf(diffs, Util.DebugLevel.DEBUG);

		int ballNumDiff = (diffs.catches - diffs.antiCatches) - (diffs.tosses - diffs.antiTosses);
		Util.printf("ballNumDiff: " + ballNumDiff, Util.DebugLevel.DEBUG);
		int ballNumDiffPositive = (ballNumDiff > 0 ? ballNumDiff : 0);
		int ballNumDiffNegative = (ballNumDiff < 0 ? ballNumDiff : 0);

		Util.printf("this: ", Util.DebugLevel.DEBUG);
		Util.printf(transition, Util.DebugLevel.DEBUG);
		Util.printf("", Util.DebugLevel.DEBUG);

		int debugCounter = 20;

		// find the transition!
		while(b < minLength || diffs.tosses != 0 || diffs.antiTosses != 0 || futureCatches + ballNumDiffNegative != diffs.catches || futureAnticatches + ballNumDiffPositive != diffs.antiCatches) {
			Util.printf(">>>>>  b: " + b, Util.DebugLevel.DEBUG);
			transition.appendEmptyBeat();
			// see if we can catch new balls/antiballs
			for(int h=0; h<numHands; h++) {
				if(this.from.getChargeAtBeatAtHand(0,h) == 0) {
					Util.printf(this.to.getChargeAtBeatAtHand(0,h), Util.DebugLevel.DEBUG);
					if(ballNumDiffNegative < 0 && this.to.getChargeAtBeatAtHand(0,h) < 0) {
						Util.printf("catching new antiball at beat " + b, Util.DebugLevel.DEBUG);
						transition.addInfiniteAntitoss(b, h, InfinityType.NEGATIVE_INFINITY);
						this.from.decChargeOfNowNodeAtHand(h);
						ballNumDiffNegative++;
					} else if(ballNumDiffPositive > 0 && this.to.getChargeAtBeatAtHand(0,h) > 0) {
						Util.printf("catching new ball at beat " + b, Util.DebugLevel.DEBUG);
						transition.addInfiniteToss(b, h, InfinityType.NEGATIVE_INFINITY);
						this.from.incChargeOfNowNodeAtHand(h);
						ballNumDiffPositive--;
					}
				}
			}
			// shift goal state backward by one beat, and match lengths
			Util.printf("shifting", Util.DebugLevel.DEBUG);
			this.to.shiftBackward();
			this.from.getFiniteNode(this.to.finiteLength() - 1);
			Util.printf("s1: " + this.from.toString(), Util.DebugLevel.DEBUG);
			Util.printf("s2: " + this.to.toString(), Util.DebugLevel.DEBUG);

			// make tosses to match charges in nodes between states
			for(int h=0; h<numHands; h++) {
				int chargeAtHand = this.from.getChargeAtBeatAtHand(0, h);
				while(chargeAtHand > 0) {
					Util.printf("performing toss at beat " + b, Util.DebugLevel.DEBUG);
					transition.addInfiniteToss(b, h, InfinityType.POSITIVE_INFINITY);
					chargeAtHand--;
					if(ballNumDiffNegative < 0 && diffs.catches == 0)
						ballNumDiffNegative++;
					else
						futureCatches++;
				}
				while(chargeAtHand < 0) {
					Util.printf("performing antitoss at beat " + b, Util.DebugLevel.DEBUG);
					transition.addInfiniteAntitoss(b, h, InfinityType.POSITIVE_INFINITY);
					chargeAtHand++;
					if(ballNumDiffPositive > 0 && diffs.antiCatches == 0)
						ballNumDiffPositive--;
					else
						futureAnticatches++;
				}
			}
			Util.printf("advancing time", Util.DebugLevel.DEBUG);
			this.from.advanceTime();
			this.to.advanceTime();
			b++;

			Util.printf("s1: " + this.from.toString(), Util.DebugLevel.DEBUG);
			Util.printf("s2: " + this.to.toString(), Util.DebugLevel.DEBUG);
			diffs = this.from.diffSums(this.to);
			Util.printf(diffs, Util.DebugLevel.DEBUG);
			Util.printf("futureCatches: " + futureCatches, Util.DebugLevel.DEBUG);
			Util.printf("futureAnticatches: " + futureAnticatches, Util.DebugLevel.DEBUG);
			Util.printf("ballNumDiffPositive: " + ballNumDiffPositive, Util.DebugLevel.DEBUG);
			Util.printf("ballNumDiffNegative: " + ballNumDiffNegative, Util.DebugLevel.DEBUG);
			Util.printf(transition, Util.DebugLevel.DEBUG);
			debugCounter--;
			if(debugCounter == 0) {
				Util.printf("debug counter threshhold reached; aborting", Util.DebugLevel.DEBUG);
				break;
			}
		}

		transition.setEventualPeriod(b);
		transition.appendEmptyBeat();
		Util.printf(transition, Util.DebugLevel.DEBUG);

		Util.printf("FINDING CATCHES!", Util.DebugLevel.DEBUG);

		// find catches!
		while(this.from.finiteLength() > 0) {
			for(int h=0; h<numHands; h++) {
				int diff = this.to.getChargeAtBeatAtHand(0, h) - this.from.getChargeAtBeatAtHand(0, h);
				if(diff > 0) {
					Util.printf("catching ball at beat " + b, Util.DebugLevel.DEBUG);
					transition.addInfiniteToss(b, h, InfinityType.NEGATIVE_INFINITY);
				} else if(diff < 0) {
					Util.printf("catching antiball at beat " + b, Util.DebugLevel.DEBUG);
					transition.addInfiniteAntitoss(b, h, InfinityType.NEGATIVE_INFINITY);
				}
			}
			b++;
			transition.appendEmptyBeat();
			this.from.advanceTime();
			this.to.advanceTime();
		}
		Util.printf("found general transition:", Util.DebugLevel.DEBUG);
		Util.printf(transition, Util.DebugLevel.DEBUG);
		Util.printf("-", Util.DebugLevel.DEBUG);
		return transition;
	}

}
