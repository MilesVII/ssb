package com.milesseventh.seabattle;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class MenuFragment extends DialogFragment {
	Game host;
	public MenuFragment(Game nhost){
		host = nhost;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		Builder builder = new Builder(getActivity());
		try {
			builder.setItems(new String[]{"Reset game", "Kill connection"},  new OnClickListener(){
				@Override
				public void onClick(DialogInterface idoignoreumf, int i) {
					switch(i){
					case 0:
						host.resetSave();
						host.restart = true;
					case 1:
						if (host.client != null)
							host.client.close();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.create();
	}
}
