package com.database.cinemaAndroid;

import java.util.ArrayList;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.ads.AdSize;
import com.database.cinemaAndroid.activities.MovieListActivityFragment.OnListaSelectedListener;
import com.database.cinemaAndroid.managers.ApplicationStatus;
import com.database.cinemaAndroid.managers.ConnectionManager;
import com.database.cinemaAndroid.managers.DetallesCompra;
import com.database.cinemaAndroid.managers.LoginManager;
import com.database.cinemaAndroid.managers.MovieManager;
import com.database.cinemaAndroid.managers.PrefManager;
import com.database.cinemaAndroid.objects.Movie;
import com.database.cinemaAndroid.utilities.Metrics;
import com.database.cinemaAndroid.utilities.Zonas;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.acra.*;
import org.acra.annotation.*;

/**
 * This class was modified to be able to be shared online via GitHub. 
 * It renders a movie list
 * 
 * Android  
 * Activity
 */
public class MovieListActivity extends Activity {
	
	private ArrayList<MovieEntry> pelis = null;
	private ArrayList<Movie> moviesArray;
	private IconListViewAdapter m_adapter;
	private Zonas zona;	
	private ListView lv = null;
	ProgressDialog pd;	
	
	private static final int ID_SELECT     	= 1;
	private static final int ID_REFRESH   	= 2;
	private static final int ID_LOGOUT		= 3;	
	private static final int ID_ACERCA 		= 4;
	private static final int ID_COMPARTIR 	= 5;
	private static final int ID_ESTRENOS 	= 6;

	private static final String Flurry_API_Key = "MYKEY";
	
	Gallery myGallery;
	
	int posterHeight;
	int posterWidth;
	int key;
	
	View selected = null;
	int colortmp;
	
	OnListaSelectedListener onListaSelected;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		int tim;
		
    	super.onCreate(savedInstanceState);

		//Flurry Start Session
		FlurryAgent.init(this, Flurry_API_Key);
		FlurryAgent.onStartSession(this);
    	
    	Bundle bundle = getIntent().getExtras();
    	//Handling an additional parameter
    	tim = (int) bundle.getInt("timeout"); 
    	if ( tim == -1) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("Error en la conexion, intente la recarga de nuevo")
    		.setTitle("Atencion!")
    		.setCancelable(false)
    		.setNegativeButton("Recargar", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					ConnectionManager.getInstance().clearCache(null);
					MovieManager.getInstance().needToRefresh = true;
					new RefreshTask().execute("");
					dialog.cancel();
				}
			});
    		AlertDialog alert = builder.create();
    		alert.show();
    	}

	}
	
	@Override
	public void onResume(){
		super.onResume();

		if(ApplicationStatus.getStatus() == false){
    		Intent reLaunchMain = new Intent(this, SplashActivity.class);
    		reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    		startActivity(reLaunchMain);
    		this.finish();
    	}

		Bundle bundle = getIntent().getExtras();
    	
    	if(bundle != null && bundle.getBoolean("salir", false)){
    		MovieListActivity.this.finish();     
          	android.os.Process.killProcess(android.os.Process.myPid());
    		
    	}

		Metrics.setContext(this);
		ConnectionManager.setContext(this);
		MovieManager.getInstance().setContext(this);
		PrefManager.getInstance().setAplicationContext(this); 
		DetallesCompra.getInstance().setOrigen("Peliculas");
    	
    	if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    		if(MovieManager.getInstance().needToRefresh == true)
    			return;  
    		carrusel();
    	}else{
    		setContentView(R.layout.lista);
    		RelativeLayout home = (RelativeLayout) findViewById(R.id.botonHome);
    		RelativeLayout cines = (RelativeLayout) findViewById(R.id.botonCine);
    		RelativeLayout peliculas = (RelativeLayout) findViewById(R.id.botonPeli);
    		cines.setOnClickListener(OnClickDoSomething(cines));
    		peliculas.setOnClickListener(OnClickDoSomething(peliculas));
    		home.setOnClickListener(OnClickDoSomething(home));
    		generarMenu();
    		inicializarLista();
    		if(MovieManager.getInstance().needToRefresh == true){
    			ConnectionManager.getInstance().clearCache(null);
    			new RefreshTask().execute("");  			
    		}
    	}   
	}
	
	@SuppressWarnings("deprecation")
	private void carrusel(){
		try{
		 setContentView(R.layout.carrusel);
		}catch(Exception e){
			return;
		}
		DisplayMetrics metrics = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics(metrics);
	    posterHeight = (int) (metrics.heightPixels * 0.6);
	    posterWidth =  (int) (posterHeight * 0.8);

	    int radius;
			
		if(metrics.widthPixels > metrics.heightPixels)
			radius = metrics.widthPixels;
		else
			radius = metrics.heightPixels;

		LinearLayout fondo = (LinearLayout) findViewById(R.id.mainly);	        
		GradientDrawable g = new GradientDrawable(Orientation.TL_BR, new int[] {0xff818181,0xff0A0A0A});
		g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
		g.setGradientRadius(radius/2);
		g.setGradientCenter(0.5f, 0.5f);
		fondo.setBackgroundDrawable(g);
	     
	    MovieManager.getInstance().resetRankingSearch();
	        
		Movie pelicula;
		moviesArray = new ArrayList<Movie>();
		while((pelicula = MovieManager.getInstance().getNextMovieRank(0)) != null){        	
			moviesArray.add(pelicula);
		}
		
		myGallery = (Gallery) findViewById(R.id.gallery1);
		
		myGallery.setAdapter(new ImageAdapter(this));

		myGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View v,
					int position, long id) {
				TextView nombrePeli = (TextView) findViewById(R.id.textView1);
				if(nombrePeli != null)
					nombrePeli.setText(moviesArray.get(position).getSpanishTitle());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
								
			}
		});
		
		myGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {					

				int movid = moviesArray.get(position).getId();
				
				Intent myIntent = new Intent(MovieListActivity.this, MainActivity.class);
				myIntent.putExtra("action", MainActivity.ID_DETAILS);				
				myIntent.putExtra("param", movid);			
				myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 
				myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(myIntent);			

			}
	    		
			});   
	}
	
	public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;
        private Context mContext;
        
        public ImageAdapter(Context c) {
            mContext = c;
            TypedArray a = obtainStyledAttributes(R.styleable.HelloGallery);
            mGalleryItemBackground = a.getResourceId(
                    R.styleable.HelloGallery_android_galleryItemBackground, 0);
            a.recycle();
        }
		
        @Override
		public int getCount() {
            return moviesArray.size();
        }
        @Override
		public Object getItem(int position) {        	
            return position;
        }
        @Override
		public long getItemId(int position) {
            return position;
        }
        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);
            if(moviesArray.get(position).posterlsLoaded() == true){
            	i.setImageDrawable( moviesArray.get(position).getPoster());            	
            }else{
            	i.setImageResource(R.drawable.image_na);
            	new DownloadImageTask2().execute(String.valueOf(position));
            }
                    
            i.setLayoutParams(new Gallery.LayoutParams(posterWidth, posterHeight));
            i.setScaleType(ImageView.ScaleType.FIT_XY);
            i.setBackgroundResource(mGalleryItemBackground);
            return i;
        }
    }
	
	private class DownloadImageTask2 extends AsyncTask<String, Void, Drawable> {
    	int index;
    	String tag;
    	
    	@Override
		protected Drawable doInBackground(String... position) {
        	index = Integer.parseInt(position[0]);
        	tag = position[0];
        	
        	moviesArray.get(index).setPoster();
        	        	            	
        	return moviesArray.get(index).getPoster();
        	
        }

        @Override
		protected void onPostExecute(Drawable poster) {  
        	ImageView imagen;
        	imagen = (ImageView) myGallery.getChildAt(index - myGallery.getFirstVisiblePosition());        	
      	
        	if(imagen!= null){
        		imagen.setImageDrawable(poster);
        		imagen.invalidate();
        	}
        }
    }
	
	View.OnClickListener OnClickDoSomething(final View v)  {
        return new View.OnClickListener() {

            @Override
			public void onClick(View v) {   

            	if(v.getId() == R.id.botonCine){
            		Intent myIntent = new Intent(MovieListActivity.this, MainActivity.class);
					myIntent.putExtra("action", MainActivity.ID_CINEMAS);							
					myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 
					myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(myIntent); 

            	}else if(v.getId() == R.id.botonPeli){
            		Toast.makeText(MovieListActivity.this, "Ya se encuentra en la lista de peliculas", Toast.LENGTH_SHORT).show();              		
        		
            	}else if(v.getId() == R.id.botonHome){
            		
            		Toast.makeText(MovieListActivity.this, "Ya se encuentra en home", Toast.LENGTH_SHORT).show();            		
            		
            	}
                 
            }
        };
    }

	public static class MovieEntry {
        public MovieEntry(Movie a) {
        	peli = a;
	
        }
      
        public Movie getPeli() {
            return peli;
        }       
        private Movie peli;      
    }

	public void inicializarLista(){
		 MovieEntry entry;
         Movie pelicula;
         
         MovieManager.getInstance().setListSelector(MovieManager.listMovies);
         
         pelis = new ArrayList<MovieEntry>();
         this.m_adapter = new IconListViewAdapter(this, R.layout.list_item_icon_text, pelis);
  
        MovieManager.getInstance().resetRankingSearch();
         
        while((pelicula = MovieManager.getInstance().getNextMovieRank(0)) != null){        	
      	      	            	
      	entry = new MovieEntry(pelicula);
      	            	            	
      	m_adapter.add(entry);      	
      	
      	}

       FrameLayout fl = (FrameLayout) findViewById(R.id.ListContainer);
   	   
       lv = new ListView(this);
       fl.addView(lv);
       
       lv.setAdapter(this.m_adapter);
       lv.setOnItemClickListener(new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			// TODO Auto-generated method stub
			int movid = (m_adapter.getItem(position)).getPeli().getId();
			Intent myIntent = new Intent(MovieListActivity.this, MainActivity.class);
			myIntent.putExtra("action", MainActivity.ID_DETAILS);				
			myIntent.putExtra("param", movid);			
			myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 
			myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(myIntent);
		}
    	   
	});

	   	PrefManager pref = PrefManager.getInstance();
	    pref.setAplicationContext(this);
	    String aux = pref.getCityWithMostSelections();
	    Log.d("CIUDAD", "" + aux);

	    String key;
		if (aux.equals("CCSE,CCSO")) {
	    	key = "ALL_CCS";
	    } else {
	    	key = "ALL_" + aux;
	    }
	    zona = new Zonas();
	    zona.createZones();
        
	}
	
	public void refreshList(){
		MovieEntry entry;
        Movie pelicula;
        
        if(m_adapter == null)
        	return;
        
		m_adapter.clear();		

	     MovieManager.getInstance().resetRankingSearch();
	     
	     while((pelicula = MovieManager.getInstance().getNextMovieRank(0)) != null){
	    	 entry = new MovieEntry(pelicula);
	    	 m_adapter.add(entry); 
	      	
	       }   
	     
	     m_adapter.notifyDataSetChanged();
		
		
	}
	
	public class IconListViewAdapter extends ArrayAdapter<MovieEntry> {

        private ArrayList<MovieEntry> items;
        

        public IconListViewAdapter(Context context, int textViewResourceId, ArrayList<MovieEntry> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                
        	View v = lv.getChildAt(position - lv.getFirstVisiblePosition());
        	LinearLayout contenedor;    
                if (v == null) {
                	
                    LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.list_item_icon_text, null);  
                    
                    }
                
                contenedor = (LinearLayout) v.findViewById(R.id.contenedor);
                
                if(position%2 == 0){                	
                	contenedor.setBackgroundResource(R.drawable.custom_button_dos);
                }
                else{
                	contenedor.setBackgroundResource(R.drawable.custom_button_tres);
                }
                   
                    
                    Movie pelicula = items.get(position).getPeli();
                    
                    if (pelicula != null) {
                    	
                    	//Fill up the list with some elements

                    		ViewHolder holder = null;                    		
                    		TextView tesp = null;
                    		TextView torg = null;  
                            ImageView im = null;
                    		
                    		if(v.getTag() == null){
                    			holder = new ViewHolder();
                    			holder.icon = (ImageView) v.findViewById(R.id.icon);
                    			holder.tituloesp = (TextView) v.findViewById(R.id.titulo_espanol);
                    			holder.tituloorg = (TextView) v.findViewById(R.id.titulo_original);

                    			tesp = holder.tituloesp;
                    			torg = holder.tituloorg;
                    			im = holder.icon;
                    			v.setTag(holder);
                    		}else{
                    			holder = (ViewHolder) v.getTag();
                    			tesp = holder.tituloesp;
                    			torg = holder.tituloorg;
                    			im = holder.icon;
                    		}
                    	
                    		 if(tesp != null){
                             	tesp.setText(pelicula.getSpanishTitle());
                             }
                             
                             if(torg != null){
                             	torg.setText(pelicula.getOriginalTitle());
                             }
                            
                            if (im!= null) {                        	
                            	if(pelicula.posterlsLoaded() == true){                        		
                            		im.setImageDrawable(pelicula.getPoster());
                            	}else{
                            		new DownloadImageTask().execute(position);
                            		
                            	}                 	
                            	                     	
                            }        
                            
                            if(position == 2){
                            	PrefManager pref = new PrefManager();
                                pref.setAplicationContext(getBaseContext());
                                String aux = pref.getCityWithMostSelections();
                                Log.d("CIUDAD", "" + aux);
                                
                                String key = "";
                                if (aux.equals("CCSE,CCSO")) {
                                	key = "ALL_CCS";
                                } else {
                                	key = "ALL_" + aux;
                                }
                                Zonas zona = new Zonas();
                                zona.createZones();

								//Admob
								RelativeLayout rl = ((RelativeLayout) v.findViewById(R.id.addViewContainer));
								AdView mAdView = new AdView(getContext());
								mAdView.setAdSize(AdSize.BANNER);
								mAdView.setAdUnitId(getResources().getString(R.string.banner_ad_unit_id));
								mAdView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Metrics.dpToPixel(55)));
								rl.addView(mAdView);
								AdRequest adRequest = new AdRequest.Builder().build();
								mAdView.loadAd(adRequest);
								((RelativeLayout) v.findViewById(R.id.addViewContainer)).setVisibility(View.VISIBLE);

                            }
                                                                
                    }
                return v;
        }
        
        private class ViewHolder {
        	  TextView tituloesp;
      	      TextView tituloorg;        	  
        	  ImageView icon;        	  
        	}
                 
        
	}
	
	private class DownloadImageTask extends AsyncTask<Integer, Void, Drawable> {
    	int index;
    	
    	
    	@Override
		protected Drawable doInBackground(Integer... position) {
        	index = position[0];        	
        	
        	(m_adapter.getItem(index)).getPeli().setPoster();
        	            	
        	return (m_adapter.getItem(index)).getPeli().getPoster();
        	
        }

        @Override
		protected void onPostExecute(Drawable poster) {  
        	View itemView;        		
        	
        	
        	if(lv != null)
        		itemView = lv.getChildAt(index - lv.getFirstVisiblePosition());
        	else
        		return;
            
        	if (itemView != null) {
                ImageView itemImageView = (ImageView) itemView.findViewById(R.id.icon);
                if(itemImageView != null)
                	itemImageView.setImageDrawable(poster);
            }          	
        	

        }
    }
	
	
	
	private void generarMenu(){	
		
		ActionItem refreshItem = new ActionItem(ID_REFRESH, "\nRefrescar\n", null);
		ActionItem estrenosCinemaItem = new ActionItem(ID_ESTRENOS, "\nPrï¿½ximos Estrenos\n", null);		
		ActionItem selectCinemaItem = new ActionItem(ID_SELECT, "\nCines Favoritos\n", null);	
		ActionItem compartirItem = new ActionItem(ID_COMPARTIR, "\nCompartir\n", null);
		ActionItem logoutItem = new ActionItem(ID_LOGOUT, "\nCerrar Sesiï¿½n\n", null);		
		ActionItem acercaDe = new ActionItem(ID_ACERCA, "\nAcerca de\n", null);
		
		final QuickAction quickAction = new QuickAction(this, QuickAction.VERTICAL);
		
		quickAction.addActionItem(refreshItem);
		quickAction.addActionItem(estrenosCinemaItem);
		quickAction.addActionItem(selectCinemaItem);
		quickAction.addActionItem(compartirItem);
		quickAction.addActionItem(logoutItem);		
		quickAction.addActionItem(acercaDe);

		quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {				
				ActionItem actionItem = quickAction.getActionItem(pos);
				Intent myIntent;
				
				//here we can filter which action item was clicked with pos or actionId parameter
				if (actionId == ID_REFRESH) {
					if(ConnectionManager.checkConn() == false){
						mensajeError("No hay conexion a internet.");
						return;
		        	}
					if(PrefManager.getInstance().isAnyCinemaChecked() == false){
						mensajeError("No hay cines favoritos, por favor seleccione al menos un cine favorito");
				        return;
					}
					ConnectionManager.getInstance().clearCache(null);
					MovieManager.getInstance().needToRefresh = true;
					new RefreshTask().execute("");				
 
				} else if (actionId == ID_LOGOUT) {
					PrefManager.getInstance().cleanUserInfo();
					LoginManager.getInstance().setLoginStatus(false);		
					Toast.makeText(getApplicationContext(), "Datos de sesion eliminados", Toast.LENGTH_SHORT).show();
				} else if (actionId == ID_SELECT) {	
					myIntent = new Intent(MovieListActivity.this, SeleccionCiudadesActivity.class);
					myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		            startActivityForResult(myIntent, 0);    
		        } else if (actionId == ID_ACERCA) {
		        	
		        	myIntent = new Intent(MovieListActivity.this, MainActivity.class);
					myIntent.putExtra("action", ID_ACERCA);								
					myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 
					myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(myIntent);		
		        	
		        } else if(actionId == ID_COMPARTIR){	
		        	Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		    		sharingIntent.setType("text/plain");
		    		String shareBody = "Descarga CinesUnidos para Android visitando:\n\n www.cinesunidos.com/aplicativo";
		    		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Descarga Cines para Unidos Android");
		    		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		    		startActivity(Intent.createChooser(sharingIntent, "Share via"));	        	
		        }else if (actionId == ID_ESTRENOS) {	
		        	if(ConnectionManager.checkConn() == false){
						mensajeError("No hay conexion a internet.");
						return;
		        	}
		        	myIntent = new Intent(MovieListActivity.this, MainActivity.class);
					myIntent.putExtra("action", ID_ESTRENOS);								
					myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 
					myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(myIntent);		
		        	
		        }
				
			}
		});	
		
		RelativeLayout preferencias = (RelativeLayout) findViewById(R.id.botonMenu); 
		preferencias.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quickAction.show(v);
			}
		});
		
		
	}
	
	
	private void mensajeError(String error){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("CinesUnidos");
	    builder.setMessage(error);       
	    builder.setPositiveButton("OK",null);
	    builder.create();
	    builder.setIcon(R.drawable.icono);
	    builder.show(); 
	}
	
	
	private class RefreshTask extends AsyncTask<String, Void, Integer> {
		 	
		 boolean resultado;	 
		 private Movie pelicula;
		 @Override
		protected void onPreExecute() {
			 
			
			 pd = ProgressDialog.show(MovieListActivity.this, "Espere.", "Actualizando listas", true, true);
			 
			 if(lv != null && m_adapter != null){
				 m_adapter.clear();
				 lv.setAdapter(m_adapter);
			 }
			 
	   	}
	    
		 @Override
		protected Integer doInBackground(String... params) {
			 
			resultado = MovieManager.getInstance().refresh();
			
			
			if(resultado == false)
				return 0;
			
			
			MovieManager.getInstance().resetRankingSearch();
			
			 while((pelicula = MovieManager.getInstance().getNextMovieRank(0)) != null){
				 if(pelicula.isInCache())
					 pelicula.setPoster();    			 
			 }
	        
	        return 0;
	    }	    

	    @Override
		protected void onPostExecute(Integer result) {
	    	
	    	MovieManager.getInstance().needToRefresh = false;
	    	if(resultado == false){
	    		try {
		    	     pd.cancel();
		    	     pd = null;
	    		} catch (Exception e) { }

	    		Toast.makeText(MovieListActivity.this, "Fallo de conexion", Toast.LENGTH_SHORT).show();
	    		refreshDialog();
	    		
	    		return;
	    	}
	    	
	    	refreshList();
	    	
	    	 try {
		    	 pd.cancel();
		         pd = null;
		        } catch (Exception e) { }
	    	
	    	
	        
	    }
	}
	
	private void refreshDialog () {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Error en la conexion, intente la recarga de nuevo")
		.setTitle("Atencion!")
		.setCancelable(false)
		.setNegativeButton("Recargar", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				ConnectionManager.getInstance().clearCache(null);
				MovieManager.getInstance().needToRefresh = true;
				new RefreshTask().execute("");
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void unbindDrawables(View view) {
		if(view == null)
			return;
	    if (view.getBackground() != null) {
	        view.getBackground().setCallback(null);
	    }
	    if (view instanceof ViewGroup) {
	        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
	            unbindDrawables(((ViewGroup) view).getChildAt(i));
	        }
	        try{
	        	((ViewGroup) view).removeAllViews();
	        }catch(UnsupportedOperationException e){
	        	
	        }
	    }
	}
	
	@Override
	public void onPause(){		

	    try {
	    	 pd.cancel();
	         pd = null;
	        } catch (Exception e) { }
		unbindDrawables(findViewById(R.id.mainly));
		
		super.onPause();

		FlurryAgent.onEndSession(this);
		
	}

	@Override
	protected void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);
	}
	
	@Override
	public void onBackPressed() {

	  AlertDialog.Builder alertDialog = new AlertDialog.Builder(this); 
      
      alertDialog.setTitle("CinesUnidos"); 
      
      alertDialog.setMessage("Esta seguro que desea salir?"); 
     
      alertDialog.setIcon(R.drawable.icono); 
     
      alertDialog.setPositiveButton("Si", new DialogInterface.OnClickListener() {
          @Override
		public void onClick(DialogInterface dialog,int which) {  
          	MovieListActivity.this.finish();     
          	android.os.Process.killProcess(android.os.Process.myPid());
          }
      }); 
      
      alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
          @Override
		  public void onClick(DialogInterface dialog, int which) {            
          
          dialog.cancel();
          }
      });

     
      alertDialog.show();    
	}
	
	@Override
	public void onDestroy()
	{	
		 
	    super.onDestroy();
	}
}