/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_cliquems;


import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.AdductInfo;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.AnClique;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.ComputeAdduct;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.ComputeCliqueModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.ComputeIsotopesModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.PeakData;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.mutable.MutableDouble;

public class CliqueMSTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(CliqueMSTask.class.getName());

  //progress constants
  public final double EIC_PROGRESS = 0.3; // EIC calculation takes about 30% time
  public final double MATRIX_PROGRESS = 0.5; // Cosine matrix calculation takes about 50% time
  public final double NET_PROGRESS = 0.1; // Network calculations takes 10% time
  public final double ISO_PROGRESS = 0.01; // Isotope calculation takes 1% time
  public final double ANNOTATE_PROGRESS = 0.09; // Adduct annotation takes 9% time


  // Feature list to process.
  private final PeakList peakList;

  // Task progress
  private final MutableDouble progress = new MutableDouble(0.0);


  // Parameters.
  private final ParameterSet parameters;

  public CliqueMSTask(final ParameterSet parameters,
      final PeakList list) {
    this.parameters = parameters;
    peakList = list;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of pseudo-spectra in " + peakList;
  }

  @Override
  public double getFinishedPercentage() {
    return progress.getValue();
  }

  @Override
  public void cancel() {
    super.cancel();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    this.progress.setValue(0.0);

    try {
      //TODO multiple rawDataFile support
      if (peakList.getRawDataFiles().size() == 0) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not calculate cliques for features " + peakList.getName()
            + " No raw datafile found for the feature");
        return;
      }
      ComputeCliqueModule cm = new ComputeCliqueModule(peakList, peakList.getRawDataFile(0),
          progress, this);
      // Check if not canceled
      if (isCanceled()) {
        return;
      }
      AnClique anClique = cm
          .getClique(parameters.getParameter(CliqueMSParameters.FILTER).getValue(),
              parameters.getParameter(CliqueMSParameters.FILTER).getEmbeddedParameters()
                  .getParameter(SimilarFeatureParameters.MZ_DIFF).getValue(),
              parameters.getParameter(CliqueMSParameters.FILTER).getEmbeddedParameters()
                  .getParameter(SimilarFeatureParameters.RT_DIFF).getValue(),
              parameters.getParameter(CliqueMSParameters.FILTER).getEmbeddedParameters()
                  .getParameter(SimilarFeatureParameters.IN_DIFF).getValue(),
              parameters.getParameter(CliqueMSParameters.TOL).getValue());
//      System.out.println("Size of peakData List 1"+anClique.getPeakDataList().size());
      // Check if not canceled
      if (isCanceled()) {
        return;
      }

      ComputeIsotopesModule cim = new ComputeIsotopesModule(anClique, this, progress);
      cim.getIsotopes(parameters.getParameter(CliqueMSParameters.ISOTOPES_MAX_CHARGE).getValue(),
          parameters.getParameter(CliqueMSParameters.ISOTOPES_MAXIMUM_GRADE).getValue(),
          parameters.getParameter(CliqueMSParameters.ISOTOPES_MZ_TOLERANCE).getValue(),
          parameters.getParameter(CliqueMSParameters.ISOTOPE_MASS_DIFF).getValue());
      // Check if not canceled
      if (isCanceled()) {
        return;
      }

      ComputeAdduct computeAdduct = new ComputeAdduct(anClique, this, this.progress);

      PolarityType polarity =
          (parameters.getParameter(CliqueMSParameters.POLARITY).getValue().equals("positive"))
              ? PolarityType.POSITIVE : PolarityType.NEGATIVE;

      List<AdductInfo> addInfos = computeAdduct.getAnnotation(
          parameters.getParameter(CliqueMSParameters.ANNOTATE_TOP_MASS).getValue(),
          parameters.getParameter(CliqueMSParameters.ANNOTATE_TOP_MASS_FEATURE).getValue(),
          parameters.getParameter(CliqueMSParameters.SIZE_ANG).getValue(),
          polarity,
          parameters.getParameter(CliqueMSParameters.ANNOTATE_TOL).getValue(),
          parameters.getParameter(CliqueMSParameters.ANNOTATE_FILTER).getValue(),
          parameters.getParameter(CliqueMSParameters.ANNOTATE_EMPTY_SCORE).getValue(),
          parameters.getParameter(CliqueMSParameters.ANNOTATE_NORMALIZE).getValue());

      // Check if not canceled
      if (isCanceled()) {
        return;
      }

      addFeatureIdentity(anClique, addInfos);

      // Finished.
      this.progress.setValue(1.0);
      logger.log(Level.FINEST,
          "Clique formation, isotope annotation and adduct annotation done for features");
      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not calculate cliques for features "+ peakList.getName()+" \n" +
          e.getMessage());
      e.printStackTrace();
    }
  }

  private void addFeatureIdentity(AnClique anClique, List<AdductInfo> addInfos) {
    List<PeakData> pdList = anClique.getPeakDataList();

    HashMap<PeakData, CliqueMSTabularPeakIdentity> pdIdentityHash = new HashMap<>();
    //Map for cliqueID to peakRowListID
    HashMap<Integer,Integer> nodeToPeakID = new HashMap<>();
    int maxID = 0;
    for(PeakData pd : pdList){
      nodeToPeakID.put(pd.getNodeID(),pd.getPeakListRowID());
      if(maxID<pd.getPeakListRowID())
        maxID = pd.getPeakListRowID();
    }
    int numberOfDigitsInMaxPeakID = 0;
    while(maxID>0){
      numberOfDigitsInMaxPeakID++;
      maxID/=10;
    }
    for (PeakData pd : pdList) {
      String cqID;
      if(nodeToPeakID.get(pd.getCliqueID()) != null)
        cqID = String.format("%0"+numberOfDigitsInMaxPeakID+"d",nodeToPeakID.get(pd.getCliqueID()));
      else if(pd.getCliqueID() != null){
        cqID = String.format("%0"+numberOfDigitsInMaxPeakID+"d",pd.getCliqueID());
      }
      else{
        cqID= "NA";
      }
      CliqueMSTabularPeakIdentity annotation = new CliqueMSTabularPeakIdentity("CliqueID "+cqID);
      annotation.addSingularProperty(PeakIdentity.PROPERTY_METHOD,"CliqueMS Algorithm");
      annotation.addSingularProperty("Isotope Annotation",pd.getIsotopeAnnotation());
      pdIdentityHash.put(pd,annotation);
      if (this.peakList.findRowByID(pd.getPeakListRowID()) != null) {
        this.peakList.findRowByID(pd.getPeakListRowID()).addPeakIdentity(annotation, true);
      }
    }
    HashMap<Integer, PeakData> pdHash = new HashMap<>();
    for (PeakData pd : pdList) {
      pdHash.put(pd.getNodeID(), pd);
    }
    for (AdductInfo adInfo : addInfos) {
      PeakData pd = pdHash.get(adInfo.feature);
      CliqueMSTabularPeakIdentity annotation = pdIdentityHash.get(pd);
      HashSet<String> adducts = new HashSet<>();
      for (int i = 0; i < ComputeAdduct.numofAnnotation; i++) {
        if(adInfo.annotations.get(i).equals("NA") || adducts.contains(adInfo.annotations.get(i)))
          continue;
        adducts.add(adInfo.annotations.get(i));
        annotation.addMultiTypeProperty("Mass",MZmineCore.getConfiguration().getMZFormat().format(adInfo.masses.get(i)));
        annotation.addMultiTypeProperty("Score", String.valueOf(adInfo.scores.get(i)));
        annotation.addMultiTypeProperty("Adduct Annotation", adInfo.annotations.get(i));
      }
    }
  }
}
