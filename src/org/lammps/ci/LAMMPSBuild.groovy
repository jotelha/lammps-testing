package org.lammps.ci
import org.lammps.ci.build.Serial
import org.lammps.ci.build.Shlib
import org.lammps.ci.build.OpenMPI
import org.lammps.ci.build.SerialClang
import org.lammps.ci.build.ShlibClang
import org.lammps.ci.build.OpenMPIClang
import org.lammps.ci.build.Documentation
import org.lammps.ci.build.Testing
import org.lammps.ci.build.TestingOMP
import org.lammps.ci.build.Regression
import org.lammps.ci.build.Intel
import org.lammps.ci.build.SerialCMake
import org.lammps.ci.build.KokkosOMP
import org.lammps.ci.build.CMakeTesting
import org.lammps.ci.build.CMakeTestingOMP
import org.lammps.ci.build.CMakeTestingGPU
import org.lammps.ci.build.CMakeTestingKokkosCUDA
import org.lammps.ci.build.Win32CrossSerialCMake
import org.lammps.ci.build.Win64CrossSerialCMake
import org.lammps.ci.build.CoverityCMake
import org.lammps.ci.build.CMakeKokkosOMP
import org.lammps.ci.build.CMakeKokkosCUDA

def regular_build(build_name, set_github_status=true, run_in_container=true, send_slack=true) {
    def docker_registry = 'http://glados2.cst.temple.edu:5000'
    def docker_image_name = 'lammps_testing:ubuntu_latest'
    def docker_args = ''
    def project_url = 'https://github.com/lammps/lammps.git'
    def testing_project_url = 'https://github.com/lammps/lammps-testing.git'
    def testing = false
    def shallow_clone = false

    switch(build_name) {
        case 'serial':
            s = new Serial('jenkins/serial/ubuntu', this)
            break
        case 'shlib':
            s = new Shlib('jenkins/shlib/ubuntu', this)
            break
        case 'openmpi':
            s = new OpenMPI('jenkins/openmpi/ubuntu', this)
            break
        case 'serial-el7':
            s = new Serial('jenkins/serial/el7', this)
            docker_image_name = 'lammps_testing:centos_7'
            shallow_clone = true
            break
        case 'shlib-el7':
            s = new Shlib('jenkins/shlib/el7', this)
            docker_image_name = 'lammps_testing:centos_7'
            shallow_clone = true
            break
        case 'openmpi-el7':
            s = new OpenMPI('jenkins/openmpi/el7', this)
            docker_image_name = 'lammps_testing:centos_7'
            shallow_clone = true
            break
        case 'coverity-scan':
            s = new CoverityCMake('jenkins/cmake/coverity', this)
            break
        case 'cmake-serial':
            s = new SerialCMake(this)
            break
        case 'cmake-kokkos-omp':
            s = new CMakeKokkosOMP(this)
            break
        case 'cmake-kokkos-cuda':
            s = new CMakeKokkosCUDA(this)
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            break
        case 'cmake-testing':
            s = new CMakeTesting(this)
            testing = true
            break
        case 'serial-clang':
            s = new SerialClang(this)
            break
        case 'shlib-clang':
            s = new ShlibClang(this)
            break
        case 'openmpi-clang':
            s = new OpenMPIClang(this)
            break
        case 'intel':
            s = new Intel(this)
            docker_image_name = 'lammps_testing:intel2018u3'
            break
        case 'build-docs':
            s = new Documentation(this)
            break
        case 'testing':
            s = new Testing(this)
            testing = true
            break
        case 'kokkos-omp':
            s = new KokkosOMP(this)
            break
        case 'testing-omp':
            s = new TestingOMP(this)
            testing = true
            break
        case 'cmake-testing-omp':
            s = new CMakeTestingOMP(this)
            testing = true
            break
        case 'cmake-testing-gpu-opencl':
            s = new CMakeTestingGPU(this, 'opencl')
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            testing = true
            break
        case 'cmake-testing-gpu-cuda':
            s = new CMakeTestingGPU(this, 'cuda')
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            testing = true
            break
        case 'cmake-testing-kokkos-cuda':
            s = new CMakeTestingKokkosCUDA(this)
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            testing = true
            set_github_status = false
            break
        case 'regression':
            s = new Regression(this)
            testing = true
            break
        case 'cmake-win32-serial':
            s = new Win32CrossSerialCMake(this)
            docker_image_name = 'lammps_testing:fedora_29_cross'
            set_github_status = false
            break
        case 'cmake-win64-serial':
            s = new Win64CrossSerialCMake(this)
            docker_image_name = 'lammps_testing:fedora_29_cross'
            set_github_status = false
            break
        default:
            currentBuild.result = 'FAILURE'
            echo 'unknown build_name ' + build_name
            return
    }

    stage('Checkout') {
        dir('lammps') {
            if(shallow_clone) {
              checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: false, reference: '', shallow: true]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'lammps-jenkins', url: project_url]]])
            } else {
              git branch: 'master', credentialsId: 'lammps-jenkins', url: project_url
            }
            git_commit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        }
        if(testing){
            dir('lammps-testing') {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'lammps-jenkins', url: testing_project_url]]])
            }
        }
        s.pre_actions()
    }

    def utils = new Utils()

    if (set_github_status) {
        utils.setGitHubCommitStatus(project_url, s.name, git_commit, 'building...', 'PENDING')
    }

    if (run_in_container) {
        def envImage = docker.image(docker_image_name)

        try {
            docker.withRegistry(docker_registry) {
                stage('Setting up build environment') {
                    // ensure image is current
                    envImage.pull()
                }

                // use workaround (see https://issues.jenkins-ci.org/browse/JENKINS-34276)
                docker.image(envImage.imageName()).inside(docker_args) {
                    timeout(time: 2, unit: 'HOURS') {
                        s.configure()
                        s.build()
                    }
                }
            }

        } catch (err) {
            echo "Caught: ${err}"
            currentBuild.result = 'FAILURE'
        }
    } else {
        try {
            s.configure()
            s.build()
        } catch (err) {
            echo "Caught: ${err}"
            currentBuild.result = 'FAILURE'
        }
    }

    s.post_actions()

    if (currentBuild.result == 'FAILURE') {
        if (set_github_status) {
            utils.setGitHubCommitStatus(project_url, s.name, git_commit, 'build failed!' + s.message, 'FAILURE')
        }
        if (send_slack) {
            slackSend color: 'bad', message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> of ${env.JOB_NAME} failed!" + s.message
        }
    } else {
        if (set_github_status) {
            utils.setGitHubCommitStatus(project_url, s.name, git_commit, 'build successful!' + s.message, 'SUCCESS')
        }
        if (send_slack) {
            slackSend color: 'good', message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> of ${env.JOB_NAME} succeeded!" + s.message
        }
    }
}

def container_build(build_name, docker_image_name, dockerfile, set_github_status=true, send_slack=true) {
    def project_url = 'https://github.com/lammps/lammps.git'
    def packages_project_url = 'https://github.com/lammps/lammps-packages.git'

    stage('Checkout') {
        dir('lammps') {
            git branch: 'master', credentialsId: 'lammps-jenkins', url: project_url
            git_commit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        }
        dir('lammps-packages') {
            git branch: 'master', credentialsId: 'lammps-jenkins', url: packages_project_url
        }
    }

    def utils = new Utils()

    if (set_github_status) {
        utils.setGitHubCommitStatus(project_url, build_name, git_commit, 'building...', 'PENDING')
    }

    stage('Build') {
        result = docker.build(docker_image_name, "-f ${dockerfile} .")
    }

    if (currentBuild.result == 'FAILURE') {
        if (set_github_status) {
            utils.setGitHubCommitStatus(project_url, build_name, git_commit, 'build failed!', 'FAILURE')
        }
        if (send_slack) {
            slackSend color: 'bad', message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> of ${env.JOB_NAME} failed!"
        }
    } else {
        if (set_github_status) {
            utils.setGitHubCommitStatus(project_url, build_name, git_commit, 'build successful!', 'SUCCESS')
        }
        if (send_slack) {
            slackSend color: 'good', message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> of ${env.JOB_NAME} succeeded!"
        }
    }
}

def pull_request(build_name) {
    def docker_registry = 'http://glados2.cst.temple.edu:5000'
    def docker_image_name = 'lammps_testing:ubuntu_latest'
    def docker_args = ''
    def project_url = 'https://github.com/lammps/lammps.git'
    def testing_project_url = 'https://github.com/lammps/lammps-testing.git'
    def testing = false

    switch(build_name) {
        case 'serial-pr':
            s = new Serial('jenkins/serial/ubuntu', this)
            break
        case 'shlib-pr':
            s = new Shlib('jenkins/shlib/ubuntu', this)
            break
        case 'openmpi-pr':
            s = new OpenMPI('jenkins/openmpi/ubuntu', this)
            break
        case 'serial-el7-pr':
            s = new Serial('jenkins/serial/el7', this)
            docker_image_name = 'lammps_testing:centos_7'
            break
        case 'shlib-el7-pr':
            s = new Shlib('jenkins/shlib/el7', this)
            docker_image_name = 'lammps_testing:centos_7'
            break
        case 'openmpi-el7-pr':
            s = new OpenMPI('jenkins/openmpi/el7', this)
            docker_image_name = 'lammps_testing:centos_7'
            break
        case 'cmake-serial-pr':
            s = new SerialCMake(this)
            break
        case 'cmake-kokkos-omp-pr':
            s = new CMakeKokkosOMP(this)
            break
        case 'cmake-kokkos-cuda-pr':
            s = new CMakeKokkosCUDA(this)
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            break
        case 'cmake-testing-pr':
            s = new CMakeTesting(this)
            testing = true
            break
        case 'serial-clang-pr':
            s = new SerialClang(this)
            break
        case 'shlib-clang-pr':
            s = new ShlibClang(this)
            break
        case 'openmpi-clang-pr':
            s = new OpenMPIClang(this)
            break
        case 'intel-pr':
            s = new Intel(this)
            docker_image_name = 'lammps_testing:intel2018u3'
            break
        case 'build-docs-pr':
            s = new Documentation(this)
            break
        case 'kokkos-omp-pr':
            s = new KokkosOMP(this)
            break
        case 'testing-pr':
            s = new Testing(this)
            testing = true
            break
        case 'testing-omp-pr':
            s = new TestingOMP(this)
            testing = true
            break
        case 'cmake-testing-omp-pr':
            s = new CMakeTestingOMP(this)
            testing = true
            break
        case 'cmake-testing-gpu-opencl-pr':
            s = new CMakeTestingGPU(this, 'opencl')
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            testing = true
            set_github_status=false
            break
        case 'cmake-testing-gpu-cuda-pr':
            s = new CMakeTestingGPU(this, 'cuda')
            docker_image_name = 'lammps_testing:ubuntu_18.04_cuda_10.0'
            docker_args = '--runtime=nvidia'
            testing = true
            set_github_status=false
            break
        case 'regression-pr':
            s = new Regression(this)
            testing = true
            break
        case 'cmake-win32-serial':
            s = new Win32CrossSerialCMake(this)
            docker_image_name = 'lammps_testing:fedora_29_cross'
            break
        case 'cmake-win64-serial':
            s = new Win64CrossSerialCMake(this)
            docker_image_name = 'lammps_testing:fedora_29_cross'
            break
        default:
            currentBuild.result = 'FAILURE'
            echo 'unknown build_name'
            return
    }

    stage('Checkout') {
        dir('lammps') {
            branch_name = "origin-pull/pull/${env.GITHUB_PR_NUMBER}/head"
            refspec = "+refs/pull/${env.GITHUB_PR_NUMBER}/head:refs/remotes/origin-pull/pull/${env.GITHUB_PR_NUMBER}/head"
            checkout([$class: 'GitSCM', branches: [[name: branch_name]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'lammps-jenkins', name: 'origin-pull', refspec: refspec, url: project_url]]])
        }

        if(testing) {
            dir('lammps-testing') {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'lammps-jenkins', url: testing_project_url]]])
            }
        }
        s.pre_actions()
    }

    gitHubPRStatus githubPRMessage('head run started')

    def envImage = docker.image(docker_image_name)

    try {
        docker.withRegistry(docker_registry) {
            stage('Setup Environment') {
                // ensure image is current
                envImage.pull()
            }

            // use workaround (see https://issues.jenkins-ci.org/browse/JENKINS-34276)
            docker.image(envImage.imageName()).inside(docker_args) {
                timeout(time: 2, unit: 'HOURS') {
                    s.configure()
                    s.build()
                }
            }
        }
        currentBuild.result = 'SUCCESS'
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
    }

    s.post_actions()

    githubPRStatusPublisher statusMsg: githubPRMessage('head run ended' + s.message), unstableAs: 'SUCCESS'

    if (currentBuild.result == 'FAILURE') {
        slackSend color: 'bad', message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> of ${env.JOB_NAME} failed!" + s.message
    } else {
        slackSend color: 'good', message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> of ${env.JOB_NAME} succeeded!" + s.message
    }
}

return this
