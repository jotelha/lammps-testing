folder('lammps/master')

def scripts = ['serial', 'serial-el7', 'shlib-el7', 'openmpi-el7', 'shlib', 'openmpi', 'serial-clang', 'shlib-clang', 'openmpi-clang', 'build-docs', 'testing', 'testing-omp', 'regression', 'intel', 'kokkos-omp']

scripts.each { name ->
    pipelineJob("lammps/master/${name}") {
        triggers {
            githubPush()
        }

        concurrentBuild(false)

        definition {
            cps {
                script(readFileFromWorkspace('pipelines/master/master.groovy'))
                sandbox()
            }
        }
    }
}

folder('lammps/master/cmake')

def cmake_scripts = ['cmake-serial', 'cmake-kokkos-omp', 'cmake-kokkos-cuda', 'cmake-testing', 'cmake-testing-omp', 'cmake-testing-gpu-opencl', 'cmake-testing-gpu-cuda', 'cmake-testing-kokkos-cuda', 'cmake-win32-serial', 'cmake-win64-serial']

cmake_scripts.each { name ->
    pipelineJob("lammps/master/cmake/${name}") {
        triggers {
            githubPush()
        }

        concurrentBuild(false)

        definition {
            cps {
                script(readFileFromWorkspace('pipelines/master/master.groovy'))
                sandbox()
            }
        }
    }
}

pipelineJob("lammps/master/cmake/coverity-scan") {
    triggers {
        cron('@weekly')
    }

    concurrentBuild(false)

    definition {
        cps {
            script(readFileFromWorkspace('pipelines/master/master.groovy'))
            sandbox()
        }
    }
}
