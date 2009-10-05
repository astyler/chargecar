package org.chargecar.gpx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.chargecar.xml.XmlHelper;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GPXReader
   {
   private final File gpxFile;
   private final List<GPXEventHandler> eventHandlers = new ArrayList<GPXEventHandler>();

   public GPXReader(final File gpxFile)
      {
      this.gpxFile = gpxFile;
      }

   public void addGPXEventHandler(final GPXEventHandler gpxEventHandler)
      {
      if (gpxEventHandler != null)
         {
         eventHandlers.add(gpxEventHandler);
         }
      }

   public void removeEventHandlers()
      {
      eventHandlers.clear();
      }

   public void read() throws IOException, JDOMException
      {
      final Element rootElement = XmlHelper.createElementNoValidate(gpxFile);

      for (final GPXEventHandler handler : eventHandlers)
         {
         handler.handleGPXBegin();
         }

      findTracks(rootElement);

      for (final GPXEventHandler handler : eventHandlers)
         {
         handler.handleGPXEnd();
         }
      }

   private void findTracks(final Element rootElement)
      {
      if (rootElement != null)
         {
         final List tracks = rootElement.getChildren(GPXFile.TRACK_ELEMENT_NAME, GPXFile.GPX_NAMESPACE);
         if ((tracks != null) && (!tracks.isEmpty()))
            {
            final ListIterator iterator = tracks.listIterator();
            while (iterator.hasNext())
               {
               final Element track = (Element)iterator.next();

               final String trackName = track.getChildText(GPXFile.NAME_ELEMENT_NAME, GPXFile.GPX_NAMESPACE);
               for (final GPXEventHandler handler : eventHandlers)
                  {
                  handler.handleTrackBegin(trackName);
                  }
               findTrackSegments(track);
               for (final GPXEventHandler handler : eventHandlers)
                  {
                  handler.handleTrackEnd(trackName);
                  }
               }
            }
         }
      }

   private void findTrackSegments(final Element trackElement)
      {
      if (trackElement != null)
         {
         final List trackSegments = trackElement.getChildren(GPXFile.TRACK_SEGMENT_ELEMENT_NAME, GPXFile.GPX_NAMESPACE);
         if ((trackSegments != null) && (!trackSegments.isEmpty()))
            {
            final ListIterator iterator = trackSegments.listIterator();
            while (iterator.hasNext())
               {
               final Element trackSegment = (Element)iterator.next();

               for (final GPXEventHandler handler : eventHandlers)
                  {
                  handler.handleTrackSegmentBegin();
                  }
               findTrackPoints(trackSegment);
               for (final GPXEventHandler handler : eventHandlers)
                  {
                  handler.handleTrackSegmentEnd();
                  }
               }
            }
         }
      }

   private void findTrackPoints(final Element trackSegment)
      {
      if (trackSegment != null)
         {
         final List trackPoints = trackSegment.getChildren(GPXFile.TRACK_POINT_ELEMENT_NAME, GPXFile.GPX_NAMESPACE);
         if ((trackPoints != null) && (!trackPoints.isEmpty()))
            {
            final ListIterator iterator = trackPoints.listIterator();
            while (iterator.hasNext())
               {
               final Element trackPointElement = (Element)iterator.next();

               final Element timeElement = trackPointElement.getChild(GPXFile.TIME_ELEMENT_NAME, GPXFile.GPX_NAMESPACE);
               final String timestamp = (timeElement != null) ? timeElement.getTextTrim() : null;

               final Element elevationElement = trackPointElement.getChild(GPXFile.ELEVATION_ELEMENT_NAME, GPXFile.GPX_NAMESPACE);
               final String elevation = (elevationElement != null) ? elevationElement.getTextTrim() : null;

               final TrackPoint trackpt = new TrackPoint(trackPointElement.getAttributeValue(GPXFile.LONGITUDE_ATTR),
                                                         trackPointElement.getAttributeValue(GPXFile.LATITUDE_ATTR),
                                                         timestamp,
                                                         elevation);

               for (final GPXEventHandler handler : eventHandlers)
                  {
                  handler.handleTrackPoint(trackpt);
                  }
               }
            }
         }
      }
   }

