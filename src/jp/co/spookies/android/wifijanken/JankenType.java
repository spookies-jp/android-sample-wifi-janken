package jp.co.spookies.android.wifijanken;

enum JankenType {
	G((int) 'g', R.drawable.gu_1, R.drawable.gu_2), C((int) 'c',
			R.drawable.choki_1, R.drawable.choki_2), P((int) 'p',
			R.drawable.pa_1, R.drawable.pa_2);
	int code;
	int selfImage;
	int enemyImage;

	private JankenType(int code, int selfImage, int enemyImage) {
		this.code = code;
		this.selfImage = selfImage;
		this.enemyImage = enemyImage;
	}

	public boolean is_draw(JankenType enemy) {
		if (this == enemy) {
			return true;
		}
		return false;
	}

	public boolean is_win(JankenType enemy) {
		if ((this == G && enemy == C) || (this == C && enemy == P)
				|| (this == P && enemy == G)) {
			return true;
		}
		return false;
	}

	public int resultBackgroundImage(JankenType enemy) {
		if (is_draw(enemy)) {
			return R.drawable.background_green;
		}
		if (is_win(enemy)) {
			return R.drawable.background_pink;
		}
		return R.drawable.background_blue;
	}

	public int resultImage(JankenType enemy) {
		if (is_draw(enemy)) {
			return R.drawable.text_draw;
		}
		if (is_win(enemy)) {
			return R.drawable.text_win;
		}
		return R.drawable.text_lose;
	}

	public static JankenType getByCode(int code) {
		for (JankenType value : values()) {
			if (value.code == code) {
				return value;
			}
		}
		return null;
	}
}
