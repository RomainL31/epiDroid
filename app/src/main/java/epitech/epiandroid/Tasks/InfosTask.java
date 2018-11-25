package epitech.epiandroid.Tasks;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import epitech.epiandroid.Databases.LoginTable;
import epitech.epiandroid.Databases.ProfilInfos;
import epitech.epiandroid.Databases.Submissions;
import epitech.epiandroid.Fragment.ProfilFragment;
import epitech.epiandroid.MyRequest;
import epitech.epiandroid.R;
import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;


public class InfosTask extends AsyncTask<Void, Void, Boolean> {
    private String token;
    private Context ctx;
    private String responseString = null;
    private Fragment parent;

    public InfosTask(Context ctx, Fragment parent) {
        this.ctx = ctx;
        LoginTable user = LoginTable.listAll(LoginTable.class).get(0);
        if (user != null)
            token = user.getToken();
        this.parent = parent;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        final String TAG = "background";
        responseString = "";
        try {
            MyRequest.clearFields();
            MyRequest.addField("token", token);
            MyRequest.CreatePost("infos");

            if (MyRequest.isStatusOk() || MyRequest.isStatusUnauthorized()) {
                responseString = MyRequest.getResponseString();
            } else if (MyRequest.isStatusTimeout()) {
                Toast.makeText(ctx, "Delai d'attente dépassé", Toast.LENGTH_SHORT).show();
            } else if (MyRequest.isStatusForbidden()) {
                throw new IOException(MyRequest.getReasonPhrase());
            } else {
                responseString = MyRequest.getResponseString();
                Log.e(TAG, "connexion failed" + responseString);
                throw new IOException(MyRequest.getReasonPhrase());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        super.onPostExecute(success);
        if (ctx != null && success) {
            JSONObject json;

            try {
                json = new JSONObject(responseString);
                if (json.has("ip")) {

                    try {
                        JSONObject board = json.getJSONObject("board");
                        try {
                            Submissions.deleteAll(Submissions.class);
                            JSONArray projets = board.getJSONArray("projets");
                            for (int i = 0; i < projets.length(); ++i) {
                                JSONObject tmp = projets.getJSONObject(i);
                                Submissions submission = new Submissions();
                                submission.setProjectName(tmp.getString("title"));
                                submission.setIdActivity(tmp.getString("id_activite"));
                                submission.setProgress(Double.valueOf(tmp.getString("timeline_barre")).intValue());
                                submission.setDate(ctx.getResources().getString(R.string.from) + " " + tmp.getString("timeline_start").substring(0, 10) + " "
                                        + ctx.getResources().getString(R.string.to) + " " + tmp.getString("timeline_end").substring(0, 10));

                                submission.save();
                            }
                        } catch (Exception ignored) {}




                        JSONObject infos = json.getJSONObject("infos");
                       
                       JSONArray current = json.getJSONArray("current");

                        ProfilInfos.deleteAll(ProfilInfos.class);
                        ProfilInfos myInfos = new ProfilInfos();

                        myInfos.setFirstName(infos.getString("firstname"));

                        myInfos.setLastName(infos.getString("lastname").toUpperCase());
                        myInfos.setLogin(infos.getString("login"));

                        myInfos.setPicUrl("https://cdn.local.epitech.eu/userprofil/profilview/" + myInfos.getLogin() + ".jpg");


                        myInfos.setActiveLog(current.getJSONObject(0).getString("active_log"));

                        myInfos.setNsLogMin(Double.valueOf(current.getJSONObject(0).getString("nslog_min")).toString());
                        myInfos.setNsLogNorm(Double.valueOf(current.getJSONObject(0).getString("nslog_norm")).toString());
                        myInfos.setSemester(infos.getString("semester_code"));
                        myInfos.save();

                        try {
                            LoginTable user = LoginTable.listAll(LoginTable.class).get(0);
                            if (user != null) {
                                user.setFirstName(infos.getString("firstname"));
                                user.setLastName(infos.getString("lastname"));
                                user.setPicUrl("https://cdn.local.epitech.eu/userprofil/profilview/" + infos.getString("picture"));
                                user.setPicUrl(myInfos.getPicUrl());


                                user.setLogin(infos.getString("login"));
                                user.setInfosUpdatedAt(System.currentTimeMillis());
                                user.save();
                            }
                        } catch (Exception ignored) {}

                        ((MaterialNavigationDrawer) ctx).getToolbar().setTitle(infos.getString("firstname"));
                        ((MaterialNavigationDrawer) ctx).getToolbar().setTitleTextColor(Color.parseColor("#DEDEDE"));
                        ((MaterialNavigationDrawer) ctx).getCurrentAccount().setTitle(infos.getString("login"));
                        ((MaterialNavigationDrawer) ctx).getCurrentAccount().setSubTitle(infos.getString("firstname") + " " + infos.getString("lastname").toUpperCase());
                        ((MaterialNavigationDrawer) ctx).notifyAccountDataChanged();

                        Picasso.with(ctx)
                                .load(myInfos.getPicUrl())
                                .transform(new CircleTransform())
                                .into((ImageView) ((Activity) ctx).findViewById(R.id.user_photo));



                        try {
                            ((ProfilFragment) parent).onProfileLoaded();
                        } catch (ClassCastException ignored) {
                        }
                      } catch (ClassCastException ignored) {}
                } else if (json.has("error")) {
                    token = ((JSONObject) json.get("error")).getString("message");
                    Toast.makeText(ctx, token, Toast.LENGTH_SHORT).show();
                } else {
                    token = "non handled error.";
                }
            } catch (JSONException e) {
                Toast.makeText(ctx, "Error while parsing server response", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }
}
