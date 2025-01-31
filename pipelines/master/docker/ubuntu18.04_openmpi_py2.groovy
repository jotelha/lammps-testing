@Library('lammps_testing')
import org.lammps.ci.LAMMPSBuild

node('atlas2') {
    def build = new LAMMPSBuild()
    def docker_image_name = "lammps/lammps:latest_ubuntu18.04_openmpi_py2"
    def dockerfile = "lammps-packages/docker/ubuntu/18.04/py2/Dockerfile"
    build.container_build(env.JOB_BASE_NAME, docker_image_name, dockerfile, true, true)
}
