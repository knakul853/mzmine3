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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * modified peakIdentity to contain a combination of key value pairs and multiple values for a
 * single key
 */
public class CliqueMSTabularPeakIdentity implements PeakIdentity {

  private String name;
  private Hashtable<String,String> singularProperties;
  private Hashtable<String, List<String>> multiProperties;


  protected CliqueMSTabularPeakIdentity(){
    this("Unknown Name");
  }

  public CliqueMSTabularPeakIdentity(String name){
    this.name = name;
    multiProperties = new Hashtable<>();
    singularProperties = new Hashtable<>();
  }

  /**
   * adds a property to a multivalue type property or initialize it if it doesnt exist
   * @param property
   * @param value
   * @return true if the property is already present, else false
   */
  public boolean addMultiTypeProperty(String property, String value){
    if(multiProperties.containsKey(property)){
      multiProperties.get(property).add(value);
      return false;
    }
    else{
      List l = new ArrayList();
      l.add(value);
      multiProperties.put(property,l);
      return true;
    }
  }

  /**
   * Adds a property to a singular value type property
   * @param property
   * @param value
   */
  public void addSingularProperty(String property, String value){
    singularProperties.put(property,value);
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString(){
    return this.getName();
  }

  @Nonnull
  @Override
  public String getDescription() {
    if( (singularProperties.size()+multiProperties.size()) == 0)
      return "";
    final StringBuilder description = new StringBuilder();
    //first show list of singular properties
    if(singularProperties.size()>0){
      description.append("<table>");
      for(String property : singularProperties.keySet()){
        description.append("<tr><th>");
        description.append(property);
        description.append("</th><td>");
        description.append(singularProperties.get(property));
        description.append("</td></tr>");
      }
      description.append("</table>");
    }


    //Table for multiple properties
    if(multiProperties.size()>0){
      description.append("<table><tr>");
      Integer size = null;
      description.append("<th>S. No.</th>");
      for(String property : multiProperties.keySet()){
        description.append("<th>");
        description.append(property);
        description.append("</th>");
        size = multiProperties.get(property).size();
      }

      description.append("</tr>");
      for(int i=0 ; i<size ; i++){
        description.append("<tr>");
        description.append("<td>");
        description.append(i+1);
        description.append("</td>");
        for(String property : multiProperties.keySet()){
          description.append("<td>");
          description.append(multiProperties.get(property).get(i));
          description.append("</td>");
        }
        description.append("</tr>");
      }
      description.append("</tr>");
    }
    return description.toString();
  }

  /**
   * Returns value of the property, if multiple value type property, this method gives all values,
   * seperated by space, if the property is present in both singular value type and multiple value
   * type, it will return all the values, else null
   * @param property
   * @return String containing one value (if singular value type property), or all the values
   * seperated by space
   */
  @Override
  public String getPropertyValue(String property) {

    final StringBuilder condensedPropertyValues = new StringBuilder();
    boolean present = false;
    if(singularProperties.containsKey(property)){
      present = true;
      condensedPropertyValues.append(singularProperties.get(property));
    }
    if(multiProperties.containsKey(property)){
      present = true;
      for(String value : multiProperties.get(property))
        condensedPropertyValues.append(value+" ");
    }
    if(!present)
      return null;
    return condensedPropertyValues.toString();
  }

  /**
   * returns all properties, singular-value type or multi-value type
   * @return
   */
  @Nonnull
  @Override
  public Map<String, String> getAllProperties() {
    Map<String,String> allProperty = new HashMap<>();
    for(String property : singularProperties.keySet()){
      allProperty.put(property,singularProperties.get(property));
    }
    for(String property : multiProperties.keySet()){
      String condensedProperty = this.getPropertyValue(property);
      allProperty.put(property,condensedProperty);
    }
    return allProperty;
  }

  /**
   * Copy the identity.
   *
   * @return the new copy.
   */

  @Override
  public synchronized   @Nonnull Object clone() {
    CliqueMSTabularPeakIdentity temp = new CliqueMSTabularPeakIdentity(this.name);
    temp.singularProperties = (Hashtable<String, String>) this.singularProperties.clone();
    temp.multiProperties = (Hashtable<String, List<String>>) this.multiProperties.clone();
    return temp;
  }
}
