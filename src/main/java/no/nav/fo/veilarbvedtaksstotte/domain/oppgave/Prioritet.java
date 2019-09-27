package no.nav.fo.veilarbvedtaksstotte.domain.oppgave;

public enum Prioritet {
	LAV("LAV"),
	NORM("NORM"),
	HOY("HOY");

	private String prioritet;

	Prioritet(String prioritet) {
		this.prioritet = prioritet;
	}

	public String getPrioritet() {
		return prioritet;
	}
}
