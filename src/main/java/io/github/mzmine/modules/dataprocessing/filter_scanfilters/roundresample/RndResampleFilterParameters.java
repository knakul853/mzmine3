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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.roundresample;

import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilterSetupDialog;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.util.ExitCode;

public class RndResampleFilterParameters extends SimpleParameterSet {

  public static final BooleanParameter SUM_DUPLICATES = new BooleanParameter(
      "Sum duplicate intensities",
      "Concatenates/sums ions count (intensity) of m/z peaks competing for being rounded at same m/z unit. "
          + "If unchecked, the intensities are averaged rather than summed.",
      false);

  public static final BooleanParameter REMOVE_ZERO_INTENSITY =
      new BooleanParameter("Remove zero intensity m/z peaks",
          "Clear all scans spectra from m/z peaks with intensity equal to zero.", true);

  public RndResampleFilterParameters() {
    super(new Parameter[] {SUM_DUPLICATES, REMOVE_ZERO_INTENSITY});
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ScanFilterSetupDialog dialog =
        new ScanFilterSetupDialog(valueCheckRequired, this, RndResampleFilter.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
