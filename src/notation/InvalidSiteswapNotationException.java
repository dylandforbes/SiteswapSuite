package siteswapsuite;

public class InvalidSiteswapNotationException extends InvalidNotationException {
	InvalidSiteswapNotationException(String s) {
		super(s);
	}
	InvalidSiteswapNotationException() {
	}
	public String getMessage() {
		return "ERROR: string `" + s + "' is not valid siteswap notation";
	}
}
