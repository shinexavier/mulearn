import os

def create_swarm_key():
    """
    Generates a new, valid swarm.key file.
    """
    key = os.urandom(32).hex()
    with open("secrets/swarm.key", "w") as f:
        f.write("/key/swarm/psk/1.0.0/\n")
        f.write(f"/base16/\n")
        f.write(f"{key}\n")

if __name__ == "__main__":
    create_swarm_key()