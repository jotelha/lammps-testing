@Library('lammps_testing')
import org.lammps.ci.LAMMPSBuild

node('atlas2') {
    def build = new LAMMPSBuild()
    def docker_image_name = "lammps/lammps:latest_centos7_openmpi_py3"
    def dockerfile = "lammps-packages/docker/centos/7/py3/Dockerfile"
    build.container_build(env.JOB_BASE_NAME, docker_image_name, dockerfile, true, true)
}
