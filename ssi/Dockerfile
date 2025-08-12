FROM ghcr.io/openwallet-foundation/acapy-agent:py3.12-1.2.0
USER root
# remove any previously cached/older plugin and install from GitHub
RUN python -m pip uninstall -y acapy-did-indy || true \
 && python -m pip install --no-cache-dir git+https://github.com/Indicio-tech/acapy-did-indy@main
USER aries
