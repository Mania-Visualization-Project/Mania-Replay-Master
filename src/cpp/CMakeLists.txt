cmake_minimum_required(VERSION 3.15)
project(render)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -O3 -Wall")
# JNI
set(CMAKE_SHARED_LINKER_FLAGS "-Wl,--add-stdcall-alias")
include_directories("C:\\JDK8\\include" "C:\\JDK8\\include\\win32")
# OpenCV
set(OpenCV_DIR "G:\\course\\cv\\opencv_build\\install")
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake/")
find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})
set(OpenCV_LIBS opencv_core opencv_imgproc opencv_highgui opencv_imgcodecs)
add_library(render SHARED render.cpp)
# linking
target_link_libraries(render ${OpenCV_LIBS})