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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.main.MZmineCore;

public class Ms2Identity extends SimplePeakIdentity {

  public Ms2Identity(final Feature featureA, final Feature featureB, Ms2SearchResult searchResult) {

    super("MS2similarity" + " m/z:"
        + MZmineCore.getConfiguration().getMZFormat().format(featureB.getMZ()) + " RT:"
        + MZmineCore.getConfiguration().getRTFormat().format(featureB.getRT()) + " Score:"
        + String.format("%3.1e", searchResult.getScore()) + " NumIonsMatched:"
        + searchResult.getNumIonsMatched() + " MatchedIons:"
        + searchResult.getMatchedIonsAsString());

    setPropertyValue(PROPERTY_METHOD, "MS2 search");
  }
}
