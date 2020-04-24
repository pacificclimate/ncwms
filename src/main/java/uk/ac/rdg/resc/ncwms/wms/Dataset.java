package uk.ac.rdg.resc.ncwms.wms;

import java.util.Collection;
import org.joda.time.DateTime;

public interface Dataset {
   String getId();

   String getTitle();

   String getCopyrightStatement();

   String getMoreInfoUrl();

   DateTime getLastUpdateTime();

   Layer getLayerById(String var1);

   Collection<Layer> getLayers();

   boolean isReady();

   boolean isLoading();

   boolean isError();

   Exception getException();

   boolean isDisabled();
}
