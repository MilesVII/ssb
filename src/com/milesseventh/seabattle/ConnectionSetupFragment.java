package com.milesseventh.seabattle;

import java.net.InetAddress;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class ConnectionSetupFragment extends DialogFragment {
	public static LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	public String t;
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		Builder builder = new Builder(getActivity());
		
		LinearLayout l = new LinearLayout(getActivity());
		l.setOrientation(LinearLayout.HORIZONTAL);
		l.setLayoutParams(lp);
		final EditText[] ips = {
			new EditText(getActivity()),
			new EditText(getActivity()),
			new EditText(getActivity()),
			new EditText(getActivity())	
		};
		String[] tb = {"192", "168", "43", "1"};
		for (int i = 0; i < ips.length; ++i){
			ips[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
			ips[i].setMinWidth(70);
			ips[i].setText("" + tb[i]);
			l.addView(ips[i]);
		}
		
		try {
			builder.setTitle(t).setView(l).setCancelable(false).setPositiveButton("Connect", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					final byte[] ip = tryParse(ips);
					if (ip != null){
						Game.initiator = true;
						new Thread(new Runnable(){
							@Override
							public void run() {
								try {
									Game.client.connect(15000, InetAddress.getByAddress(ip), Game.PORT);
								} catch (Exception e) {
									Game.initiator = false;
									Game.shout("Failed");
									e.printStackTrace();
								}
							}
						}).start();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.create();
	}
	
	public byte[] tryParse(EditText[] et){
		byte[] ip = {0, 0, 0, 0};
		try{
			for (int i = 0; i < et.length; ++i)
				ip[i] = (byte)(Integer.parseInt(et[i].getText().toString()));
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return ip;
	}
}
