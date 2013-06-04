package at.tomtasche.glassinator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import at.tomtasche.glassinator.Glassinator.Callback;

public class MainActivity extends Activity {

	private static final int AUTHORIZATION_CODE = 1993;
	private static final int ACCOUNT_CODE = 1601;

	private AuthPreferences authPreferences;
	private AccountManager accountManager;

	private TextView statusView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		statusView = (TextView) findViewById(R.id.text);

		accountManager = AccountManager.get(this);

		authPreferences = new AuthPreferences(this);

		// always invalidate first, because this will ensure we get a fresh (and
		// most probably working) token
		invalidateToken();

		if (authPreferences.getUser() != null
				&& authPreferences.getToken() != null) {
			doCoolAuthenticatedStuff();
		} else {
			chooseAccount();
		}
	}

	private Callback statusCallback = new Callback() {

		@Override
		public void run() {
			statusView.append(status);
			statusView.append(Glassinator.LINE_SEPARATOR);
			statusView.append("----------");
		}
	};

	private void doCoolAuthenticatedStuff() {
		Glassinator glassinator = new Glassinator(authPreferences.getToken(),
				new Handler());

		// print the current contents of the timeline
		glassinator.getTimeline(statusCallback);
		// post our own card to the timeline
		glassinator.postTimelineCard("Hey Arnold!", "get to the choppa!",
				statusCallback);
		// print contents of the timeline after posting our own card
		glassinator.getTimeline(statusCallback);
	}

	private void chooseAccount() {
		// use https://github.com/frakbot/Android-AccountChooser for
		// compatibility with older devices
		Intent intent = AccountManager.newChooseAccountIntent(null, null,
				new String[] { "com.google" }, false, null, null, null, null);
		startActivityForResult(intent, ACCOUNT_CODE);
	}

	private void requestToken() {
		Account userAccount = null;
		String user = authPreferences.getUser();
		for (Account account : accountManager.getAccounts()) {
			if (account.name.equals(user)) {
				userAccount = account;

				break;
			}
		}

		accountManager.getAuthToken(userAccount, "oauth2:" + Glassinator.SCOPE,
				null, this, new OnTokenAcquired(), null);
	}

	/**
	 * call this method if your token expired, or you want to request a new
	 * token for whatever reason. call requestToken() again afterwards in order
	 * to get a new token.
	 */
	private void invalidateToken() {
		AccountManager accountManager = AccountManager.get(this);
		accountManager.invalidateAuthToken("com.google",
				authPreferences.getToken());

		authPreferences.setToken(null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == AUTHORIZATION_CODE) {
				requestToken();
			} else if (requestCode == ACCOUNT_CODE) {
				String accountName = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				authPreferences.setUser(accountName);

				// invalidate old tokens which might be cached. we want a fresh
				// one, which is guaranteed to work
				invalidateToken();

				requestToken();
			}
		}
	}

	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

		@Override
		public void run(AccountManagerFuture<Bundle> result) {
			try {
				Bundle bundle = result.getResult();

				Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
				if (launch != null) {
					startActivityForResult(launch, AUTHORIZATION_CODE);
				} else {
					String token = bundle
							.getString(AccountManager.KEY_AUTHTOKEN);

					authPreferences.setToken(token);

					doCoolAuthenticatedStuff();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
