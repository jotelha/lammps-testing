#!/bin/bash
#
# Copyright (C) 2019 IMTEK Simulation
# Author: Johannes Hoermann, johannes.hoermann@imtek.uni-freiburg.de
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
# Simple comparison of positions, velocities and forces between two
# NetCDF trajectories expected to be equivalent.
# Requires NCO (NetCDF operators).

# Standard format of LAMMPS USER-NETCDF NetCDF trajectory:
# (ncdump -h diff.nc)
#
# netcdf diff {
# dimensions:
#   frame = UNLIMITED ; // (11 currently)
#   cell_angular = 3 ;
#   spatial = 3 ;
#   label = 10 ;
#   cell_spatial = 3 ;
#   atom = 32000 ;
# variables:
#   double cell_angles(frame, cell_angular) ;
#     cell_angles:units = "degree" ;
#   char cell_angular(spatial, label) ;
#   double cell_lengths(frame, cell_spatial) ;
#     cell_lengths:units = "lj" ;
#   double cell_origin(frame, cell_spatial) ;
#     cell_origin:units = "lj" ;
#   char cell_spatial(spatial) ;
#   float coordinates(frame, atom, spatial) ;
#   float forces(frame, atom, spatial) ;
#   int id(frame, atom) ;
#   char spatial(spatial) ;
#   double time(frame) ;
#     time:units = "lj" ;
#   float velocities(frame, atom, spatial) ;
#
# // global attributes:
#     :Conventions = "AMBER" ;
#     :ConventionVersion = "1.0" ;
#     :program = "LAMMPS" ;
#     :programVersion = "20 Nov 2019" ;
#     :history = "Fri Dec 20 15:07:47 2019: ncdiff netcdf_standard.nc netcdf_mpiio.nc diff.nc" ;
#     :NCO = "netCDF Operators version 4.7.9 (Homepage = http://nco.sf.net, Code = http://github.com/nco/nco)" ;
# }

# FORHLR2 modules, system-dependent
# module purge
# module load compiler/intel/19.0
# module load mpi/openmpi/3.1
# module load lib/netcdf/4.6
# module load lib/nco/4.7.9

# input and reference files
INP_NC=netcdf_mpiio.nc
REF_NC=netcdf_ref.nc
REF_TXT=diff_avgsqr_ref.txt

# compute difference between mpiio and standard trajectory
ncdiff -O -v coordinates,velocities,forces "${INP_NC}" "${REF_NC}" diff.nc

# compute root mean square average for trajectory difference
ncap2 -v -O -S lmp_netcdf_avgsqr.nco diff.nc diff_avgsqr.nc

# write rms sq ave variable values into textf file
ncks -v xavgsqr,vavgsqr,favgsqr -V diff_avgsqr.nc > diff_avgsqr.txt

# compare text files
diff diff_avgsqr.txt "${REF_TXT}"
if [ $? -ne 0 ]; then
    echo "UNEXPECTED RESULTS"
    echo "Input '${INP_NC}' and reference '${REF_NC}' differ."
    echo "--- ${INP_NC} avgsqr ---"
    cat diff_avgsqr.txt
    echo "--- ${REF_NC} avgsqr ---"
    cat "${REF_TXT}"
    exit 1
else
    echo "EXPECTED RESULTS"
    exit 0
fi
