package net.sf.mzmine.modules.datapointprocessing.isotopes.deisotoper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.IsotopePatternManipulator;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.FormulaUtils;

public class DPPIsotopeGrouperTask3 extends DataPointProcessingTask {

  private static Logger logger = Logger.getLogger(DPPIsotopeGrouperTask.class.getName());

  // peaks counter
  private int processedPeaks, totalPeaks;

  // parameter values
  private MZTolerance mzTolerance;
  private boolean monotonicShape;
  private int maximumCharge;
  private String elements;
  private boolean autoRemove;
  private double mergeWidth;
  private final double minAbundance = 0.01;

  public DPPIsotopeGrouperTask3(DataPoint[] dataPoints, SpectraPlot plot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, plot, parameterSet, controller, listener);

    // Get parameter values for easier use
    mzTolerance = parameterSet.getParameter(DPPIsotopeGrouperParameters.mzTolerance).getValue();
    monotonicShape =
        parameterSet.getParameter(DPPIsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameterSet.getParameter(DPPIsotopeGrouperParameters.maximumCharge).getValue();
    elements = parameterSet.getParameter(DPPIsotopeGrouperParameters.element).getValue();
    autoRemove = parameterSet.getParameter(DPPIsotopeGrouperParameters.autoRemove).getValue();
  }


  @Override
  public void run() {
    if (!(getDataPoints() instanceof ProcessedDataPoint[])) {
      logger.warning(
          "The data points passed to Isotope Grouper were not an instance of processed data points. Make sure to run mass detection first.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // check formula
    if (!FormulaUtils.checkMolecularFormula(elements)) {
      setStatus(TaskStatus.ERROR);
      logger.warning("Invalid element parameter in " + this.getClass().getName());
    }

    ExtendedIsotopePattern[] elementPattern =
        getIsotopePatterns(elements, mergeWidth, minAbundance);

    ProcessedDataPoint[] originalDataPoints = (ProcessedDataPoint[]) getDataPoints();

    // one loop for every element
    for (ExtendedIsotopePattern pattern : elementPattern) {

      // one loop for every datapoint
      // we want to check all the isotopes for every datapoint before we delete anything. this will
      // take a long time, but should give the most reliable results.
      // we search by ascending mz
      for (int i_dp = 0; i_dp < dataPoints.length; i_dp++) {

        // dp is the peak we are currently searching an isotope pattern for
        ProcessedDataPoint dp = originalDataPoints[i_dp];

        int numIsotopes = pattern.getDataPoints().length;

        Double bestppm[] = new Double[numIsotopes];
        ProcessedDataPoint[] bestdp = new ProcessedDataPoint[numIsotopes];

        BufferElement buffer[] = new BufferElement[numIsotopes];

        // we just assume the mass is high enough that the added isotope mass does not make too much
        // of a difference, if we just take the upper endpoint as width
        mergeWidth = mzTolerance.getToleranceRange(dp.getMZ()).upperEndpoint();

        // in this loop we go though every isotope and check if we can find a peak that fits for
        // every isotope
        for (int isotopeindex = 1; isotopeindex < numIsotopes; isotopeindex++) {

          // this is the mass difference the current isotope peak would add to the base peak.
          double isoMzDiff =
              pattern.getDataPoints()[isotopeindex].getMZ() - pattern.getDataPoints()[0].getMZ();

          // look for isotope peaks in the spectrum by ascending mz
          for (int j_p = i_dp + 1; j_p < dataPoints.length; j_p++) {

            ProcessedDataPoint p = originalDataPoints[j_p];

            // if the data point p is below the mz tolerance range, we go for the next one.
            if (p.getMZ() < mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff).lowerEndpoint())
              continue;

            // if the m/z of this data point (p) is bigger than the m/z of (dp + pattern width +
            // merge) then we don't need to check anymore
            if (p.getMZ() > mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff).upperEndpoint())
              break;

            // now check the ppm difference and compare the mass differences
            double ppm = ppmDiff(p.getMZ(), dp.getMZ() + isoMzDiff);
            if (bestppm[isotopeindex] == null) {
              bestppm[isotopeindex] = new Double(ppm);
              bestdp[isotopeindex] = p;
            } else if (bestppm[isotopeindex] != null
                && Math.abs(ppm) < Math.abs(bestppm[isotopeindex].doubleValue())) {
              bestppm[isotopeindex] = ppm;
              bestdp[isotopeindex] = p;
            }

          } // end of ascending datapoint mz loop
          // now we checked all peaks upcoming peaks in the spectrum

          // also, if we previously assigned an isotope pattern, we also need to check that
          if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
            List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);

            // we go through every isotope pattern that has been assigned already
            for (int k_pr = 0; k_pr < patternResults.size(); k_pr++) {
              DPPIsotopePatternResult result = (DPPIsotopePatternResult) patternResults.get(k_pr);

              IsotopePattern resultPattern = result.getValue();
              DataPoint[] patternDPs = resultPattern.getDataPoints();

              // check all the data points in range of the peak we are looking for
              for (int l_pdp = 0; l_pdp < patternDPs.length; l_pdp++) {
                DataPoint patternDP = patternDPs[l_pdp];

                if (patternDP.getMZ() < mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff)
                    .lowerEndpoint())
                  continue;
                if (patternDP.getMZ() > mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff)
                    .upperEndpoint())
                  break;

                double ppm = ppmDiff(patternDP.getMZ(), dp.getMZ() + isoMzDiff);
                ProcessedDataPoint linkedDp = result.getLinkedDataPoint(l_pdp);
                if (bestppm[isotopeindex] == null && linkedDp != null) {
                  bestppm[isotopeindex] = new Double(ppm);
                  bestdp[isotopeindex] = linkedDp;
                } else if (bestppm[isotopeindex] != null
                    && Math.abs(ppm) < Math.abs(bestppm[isotopeindex].doubleValue())
                    && linkedDp != null) {
                  bestppm[isotopeindex] = ppm;
                  bestdp[isotopeindex] = linkedDp;
                }
              } // end of pattern datapointarray loop
            } // end of isotope pattern results loop
          }

        }
        // ok we finished looking for the isotope peaks, let's see what we found

        // check if results are good, we must have found a peak for every isotope of the current
        // element, else we have to discard the results
        boolean resultsGood = true;
        for (int isotopeindex = 0; isotopeindex < numIsotopes; isotopeindex++) {
          if (bestppm[isotopeindex] == null || bestdp[isotopeindex] == null) {
            resultsGood = false;
            break;
          }
        }
        // ok every peak has been found, now assign the pattern and link the data points
        if (resultsGood) {
          IsotopePattern patternx = new SimpleIsotopePattern(bestdp, IsotopePatternStatus.DETECTED,
              pattern.getDescription());
          dp.addResult(new DPPIsotopePatternResult(patternx, bestdp));
          logger.info("Found isotope pattern of element " + patternx.getDescription()
              + " at base m/z " + dp.getMZ());
        }


      } // end of all datapoints loop

    } // end of all elements loop

    // now we looped through all dataPoints and link the found isotope patterns together
    // we start from the back so we can just accumulate them, by linking the peaks
    for (int i = originalDataPoints.length - 1; i >= 0; i--) {
      ProcessedDataPoint dp = originalDataPoints[i];

      if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {

      }
    }

  } 

  public class BufferElement {
    final double ppm;
    final ProcessedDataPoint dp;

    BufferElement(double ppm, ProcessedDataPoint dp) {
      this.ppm = ppm;
      this.dp = dp;
    }

    public double getPpm() {
      return ppm;
    }

    public ProcessedDataPoint getDp() {
      return dp;
    }
  }

  public static double ppmDiff(double realmz, double calcmz) {
    return 10E6 * (realmz - calcmz) / calcmz;
  }

  public static IsotopePattern checkOverlappingIsotopes(IsotopePattern pattern, IIsotope[] isotopes,
      double mergeWidth, double minAbundance) {
    DataPoint[] dp = pattern.getDataPoints();
    double basemz = dp[0].getMZ();
    List<DataPoint> newPeaks = new ArrayList<DataPoint>();

    double isotopeBaseMass = 0d;
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() > minAbundance) {
        isotopeBaseMass = isotope.getExactMass();
        logger.info("isotopeBaseMass of " + isotope.getSymbol() + " = " + isotopeBaseMass);
        break;
      }
    }


    // loop all new isotopes
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() < minAbundance)
        continue;
      // the difference added by the heavier isotope peak
      double possiblemzdiff = isotope.getExactMass() - isotopeBaseMass;
      if (possiblemzdiff < 0.000001)
        continue;
      boolean add = true;
      for (DataPoint patternDataPoint : dp) {
        // here check for every peak in the pattern, if a new peak would overlap
        // if it overlaps good, we dont need to add a new peak

        int i = 1;
        do {
          if (Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff) <= mergeWidth) {
            // TODO: maybe we should do a average of the masses? i can'T say if it makes sense,
            // since
            // we're just looking for isotope mass differences and dont look at the total
            // composition,
            // so we dont know the intensity ratios
            logger.info("possible overlap found: " + i + " * pattern dp = "
                + patternDataPoint.getMZ() + "\toverlaps with " + isotope.getMassNumber()
                + isotope.getSymbol() + " (" + (isotopeBaseMass - isotope.getExactMass())
                + ")\tdiff: " + Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff));
            add = false;
          }
          i++;
          // logger.info("do");
        } while (patternDataPoint.getMZ() * i <= possiblemzdiff + mergeWidth
            && patternDataPoint.getMZ() != 0.0);
      }

      if (add)
        newPeaks.add(new SimpleDataPoint(possiblemzdiff, 1));
    }

    // now add all new mzs to the isotopePattern
    // DataPoint[] newDataPoints = new SimpleDataPoint[dp.length + newPeaks.size()];
    for (DataPoint p : dp) {
      newPeaks.add(p);
    }
    newPeaks.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return new SimpleIsotopePattern(newPeaks.toArray(new DataPoint[0]),
        IsotopePatternStatus.PREDICTED, "");
  }

  /**
   * Returns an array of isotope patterns for the given string. Every element gets its own isotope
   * pattern.
   * 
   * @param elements String of element symbols
   * @param mergeWidth
   * @param minAbundance
   * @return
   */
  public static ExtendedIsotopePattern[] getIsotopePatterns(String elements, double mergeWidth,
      double minAbundance) {
    SilentChemObjectBuilder builder =
        (SilentChemObjectBuilder) SilentChemObjectBuilder.getInstance();
    IMolecularFormula form =
        MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(elements, builder);

    ExtendedIsotopePattern[] isotopePatterns = new ExtendedIsotopePattern[form.getIsotopeCount()];

    int i = 0;
    // create a isotope pattern for every element
    for (IIsotope element : form.isotopes()) {
      isotopePatterns[i] =
          (ExtendedIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(
              element.getSymbol(), minAbundance, mergeWidth, 1, PolarityType.NEUTRAL, true);
      i++;
    }
    return isotopePatterns;
  }

  private static String dataPointsToString(DataPoint[] dp) {
    String str = "";
    for (DataPoint p : dp)
      str += "(" + p.getMZ() + ", " + p.getIntensity() + "), ";
    return str;
  }

  @Override
  public String getTaskDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    // TODO Auto-generated method stub
    return 0;
  }

}
