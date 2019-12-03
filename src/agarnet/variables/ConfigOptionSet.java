/* This file is part of 'Subopt'
 *
 * Copyright (C) 2010 Paul Jakma
 *
 * Subopt is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * Subopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with Subopt.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.variables;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import agarnet.variables.atoms.ObjectVar;

/* A wrapper around a map of maps, so support subopts specified by:
 * 
 * - belonging to a branchkey that must be taken
 * - keys applicable to any branch
 * 
 * */
public class ConfigOptionSet {
  private HashMap<String, HashMap<String, ObjectVar>> branchvars
    = new HashMap<String, HashMap<String, ObjectVar>> ();
  private HashMap<String, ObjectVar> vars = new HashMap<String, ObjectVar> ();
  
  public void put (String branchkey, ObjectVar [] objvars) {
    HashMap<String, ObjectVar> hm = branchvars.get (branchkey);
    
    if (hm == null)
      branchvars.put (branchkey, (hm = new HashMap<String, ObjectVar> ()));
    
    for (ObjectVar ov : objvars)
      hm.put (ov.getName (), ov);
  }
  public void put (ObjectVar [] objvars) {
    for (ObjectVar ov : objvars)
      vars.put (ov.getName (), ov);
  }
  
  public void put (String branchkey, ObjectVar ov) {
    HashMap<String, ObjectVar> hm = branchvars.get (branchkey);
    
    if (hm == null)
      branchvars.put (branchkey, (hm = new HashMap<String, ObjectVar> ()));
    
    hm.put (ov.getName (), ov);
  }
  public void put (ObjectVar ov) {
    vars.put (ov.getName (), ov);
  }
  
  public ObjectVar get (String branchkey, String suboptkey) {
    HashMap<String, ObjectVar> hm = branchvars.get (branchkey);
    ObjectVar ov;
    
    if (hm == null)
      return null;
    
    if ((ov = hm.get (suboptkey)) == null)
      ov = vars.get (suboptkey);
    return ov;
  }
  public ObjectVar get (String suboptkey) {
    return vars.get (suboptkey);
  }
  
  public int num_branches () {
    return branchvars.size () ;
  }
  
  public int num_subopts (String branch) {
    HashMap<String, ObjectVar> hm = branchvars.get (branch);
    
    if (hm == null)
      return 0;
    return hm.size ();
  }
  
  /* general number of high level keys */
  public int num_keys () {
    return vars.size () + branchvars.size ();
  }
  /* Query for all applicable keys */
  public String [] keys () {
    Set<String> keys = new HashSet<String> ();
    
    keys.addAll (branchvars.keySet ());
    keys.addAll (vars.keySet ());
    
    return Collections.unmodifiableSet (keys).toArray (new String [0]);
  }
  /* Query for keys applicable in a certain branch */
  public String [] keys (String branchkey) {
    HashMap<String, ObjectVar> hm = branchvars.get (branchkey);
    Set<String> keys;
    
    if (hm == null && vars == null)
      return null;
    
    keys = new HashSet<String> ();
    
    if (hm != null)
      keys.addAll (hm.keySet ());
    if (vars != null)
      keys.addAll (vars.keySet ());
    return Collections.unmodifiableSet (keys).toArray (new String [0]);
  }
  
  /* Just branching keys */
  public Set<String> branch_keys () {
    return Collections.unmodifiableSet (branchvars.keySet ());
  }
  /* Just the subopt keys for a particular branch */
  public Set<String> subopt_keys (String branch) {
    HashMap<String, ObjectVar> hm = branchvars.get (branch);
    
    if (hm != null)
      return Collections.unmodifiableSet (hm.keySet ());
    return null;
  }
  /* Just the general subopt keys */
  public Set<String> subopt_keys () {
    return Collections.unmodifiableSet (vars.keySet ());
  }
}
