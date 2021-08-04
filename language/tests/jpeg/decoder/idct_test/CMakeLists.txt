cmake_minimum_required(VERSION 3.10)

project(RegressionIDCT)

set(NAMESPACE jpeg.decoder.test.idct)

set(EXAMPLES_ROOT $ENV{STREAMBLOCKS_HOME}/streamblocks-examples)

include(${EXAMPLES_ROOT}/Streamblocks.cmake)

set(CAL_SOURCE_PATH
  ${CMAKE_SOURCE_DIR}/.
  ${EXAMPLES_ROOT}/system
  ${EXAMPLES_ROOT}/jpeg/decoder/parallel/idct
  ${EXAMPLES_ROOT}/jpeg/decoder/serial
)

streamblocks(
  regression_idct_sim
  PLATFORM vivado-hls
  SYSTEMC
  QID ${NAMESPACE}.${CMAKE_PROJECT_NAME}
  TARGET_PATH generated/sc/
  SOURCE_PATH ${CAL_SOURCE_PATH}
)
