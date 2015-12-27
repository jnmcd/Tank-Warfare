package tankwarfare;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
public class TankWarfare {
    public static void main(String[] args){
        World w = new World();
    }
}
class Map {
    static int size = 1000;
    static double smoothness = 0.4;
    static double jaggedness = 1.8;
    int[] heightMap = new int[size + 1];
    Area map = new Area();
    public Map(){
        int maxIterations = (int)(Math.log(1024)/Math.log(2));
        for(int i = 1; i <= maxIterations; i++){
            for(double split = 1; split < (1 << i); split++){
                int index = (int) ((split / (1 << i)) * size);
                int low = heightMap[(int) (((split - 1) / (1 << i)) * size)];
                int high = heightMap[(int) (((split + 1) / (1 << i)) * size)];
                if(heightMap[index] != 0)
                    continue;
                heightMap[index] = (low + high) / 2 + (int)((1 - (2 * Math.round(Math.random()))) * (1 - Math.pow(Math.random(), smoothness)) * Math.pow(jaggedness, maxIterations - i));
            }
        }
        Polygon p = new Polygon();
        for(int i = 0; i < size; i++)
            p.addPoint(i, heightMap[i] + 200);
        p.addPoint(size - 1, 500);
        p.addPoint(0, 500);
        map = new Area(p);
    }
    public void draw(Graphics2D g){
        g.setColor(Color.BLACK);
        Polygon p = new Polygon();
        for(int i = 0; i < size; i++)
            p.addPoint(i, heightMap[i] + 200);
        p.addPoint(size - 1, 10000);
        p.addPoint(0, 10000);
        map = new Area(p);
        g.fill(map);
    }
    public void updateHeightMap(){
    }
    public void remove(Area a1){
        for(int x = 0; x < size; x++){
            Rectangle rect = new Rectangle(x, heightMap[x] + 200, 1, 1000);
            Area a2 = new Area(rect);
            a2.intersect(a1);
            heightMap[x] += a2.getBounds().height;
	    if(heightMap[x] > 300)
		heightMap[x] = 300;
        }
    }
    public void add(Area a1){
        for(int x = 0; x < size; x++){
            Rectangle rect1 = new Rectangle(x, heightMap[x] + 200, 1, 1000);
            Rectangle rect2 = new Rectangle(x, 0, 1, 1000);
            Area a2 = new Area(rect1);
	    Area a3 = new Area(rect2);
            a2.intersect(a1);
	    a3.intersect(a1);
            heightMap[x] -= a3.getBounds().height - a2.getBounds().height;
        }
    }
}
class Tank {
    int life = 100;
    int width = 50;
    int height = 20;
    int driveLeft = 200;
    int x; //Center
    int y = 0; //Bottom of treads
    int selectedWeapon = 0;
    double bodyAngle = 0;
    double barrelAngle = 0;
    double barrelAim = -45;
    double power = 6;
    ArrayList<Bullets> bullets = new ArrayList(){{
	add(new Bullets.DeathRain());
	add(new Bullets.HoningBomb());
	add(new Bullets.DeathShaft());
        add(new Bullets.Nuke());
        add(new Bullets.ClusterBomb());
        add(new Bullets.Miner());
	add(new Bullets.BottledMountain());
	add(new Bullets.Sniper());
	add(new Bullets.FlyingTurret());
	add(new Bullets.Teleport());
    }};
    TankBarrel barrel = new TankBarrel(x, y);
    TankBody body = new TankBody(x, y);
    Rectangle center = new Rectangle();
    public Tank(int x){
	this.x = x;
        updateLocation();
    }
    public void hit(int power){
        life -= power;
        if(life < 0){
            JOptionPane.showMessageDialog(null, "Player " + (World.turn + 1) + " wins!");
            Bullets.bulletsFired.clear();
	    System.out.println(Bullets.bulletsFired.isEmpty());
            World.reset();
        }
    }
    public Area getBounds(){
        Area bounds = barrel.getArea();
        bounds.add(body.getArea());
        return bounds;
    }
    public void drive(int dir){
	if((x + Math.signum((dir)) > Map.size - width / 2) || (x + Math.signum((dir)) < width / 2))
	    return;
	if(dir < 0)
	    if(bodyAngle <  1 && driveLeft-- > 0)
		x += Math.signum(dir);
	if(dir > 0)
	    if(bodyAngle > -1 && driveLeft-- > 0)
		x += Math.signum(dir);
        updateLocation();
    }
    public final void updateLocation(){//No animation. Future improvement!
        int highestPoint = Integer.MAX_VALUE;
        int x1 = 0;
        double slope = 1000000;
        for(int X1 = x - width / 2; X1 <= x + width / 2; X1 += 1)
            if(World.map.heightMap[X1] < highestPoint){
                highestPoint = World.map.heightMap[X1];
                x1 = X1;
            }
        if(x1 < x)
            for(int x2 = x1; x2 <= x + width / 2; x2 += 5){
                if(Math.abs(x2 - x1) < 5)
                    continue;
                double thisSlope = (double)(World.map.heightMap[x2] - World.map.heightMap[x1]) / (double)(x2 - x1);
                if(Math.abs(thisSlope) < Math.abs(slope))
                    slope = thisSlope;
            }
        else {
            for(int x2 = x - width / 2; x2 <= x1; x2 += 5){
                if(Math.abs(x2 - x1) < 5)
                    continue;
                double thisSlope = (double)(World.map.heightMap[x2] - World.map.heightMap[x1]) / (double)(x2 - x1);
                if(Math.abs(thisSlope) < Math.abs(slope))
                    slope = thisSlope;
            }
        }
        y = (int)(slope * x + (slope * -x1 + World.map.heightMap[x1] + 200));
        body.setAngle(slope);
	bodyAngle = slope;
        barrel.setAngle(Math.atan(slope) + (Math.PI * barrelAim / 180));
        body.move(x, y);
        barrel.move(x, y - 15);
    }
    public void draw(Graphics2D G){
        barrel.draw(G);
        body.draw(G);
        G.setColor(Color.blue);
        G.draw(center);
    }	
    public void fire(){
	try {
	    Class<?> clazz = Class.forName(bullets.get(selectedWeapon).getClass().getName());
	    Bullets bullet = (Bullets) clazz.newInstance();
	    bullet.shoot(barrel.angle, power, barrel.getTip()[0], barrel.getTip()[1]);
	} catch(ClassNotFoundException | InstantiationException | IllegalAccessException e){
	    e.printStackTrace();
	}
	World.noTurn();
    }
    public void scroll(int dir){
	selectedWeapon += dir;
	while(selectedWeapon < 0)
	    selectedWeapon += bullets.size();
	selectedWeapon %= bullets.size();
    }
}
abstract class Bullets implements Cloneable {
    static ArrayList<Bullets> bulletsFired = new ArrayList();
    double xmove, ymove;    
    double x, y;
    boolean exploded = false;
    int power;
    String name = "";
    public void shoot(double angle, double power, double x, double y){
        ymove = power * Math.sin(angle);
        xmove = power * Math.cos(angle);
        this.x = x;
        this.y = y;
        bulletsFired.add(this);
    }
    public void move(){
        x += xmove / 2;
        y += ymove / 2;
        ymove += 0.2 / 2;
        checkCollisions();
    }
    public void draw(Graphics2D G){
        G.setColor(Color.BLACK);
        G.fillOval((int)(x - 2.5), (int)(y - 2.5), 5, 5);
    }
    public void checkCollisions(){
        Polygon bounds = new Polygon();
        for(int i = 0; i < 360; i += 10){
            double pX = (2.5 * Math.cos(i * Math.PI / 180));
            double pY = (2.5 * Math.sin(i * Math.PI / 180));
            bounds.addPoint((int)(x + pX), (int)(y + pY));
        }
        Area minusGround = new Area(bounds);
        minusGround.intersect(World.map.map);
        Area explosion = new Area(bounds);
        explosion.intersect(World.player1.getBounds());
        if(!explosion.isEmpty())
            makeBOOM();
        explosion = new Area(bounds);
        explosion.intersect(World.player2.getBounds());
        if(!explosion.isEmpty() && !exploded)
            makeBOOM();
        if(!minusGround.isEmpty() && !exploded)
            makeBOOM();
        if(x < 0 || x > Map.size || y > 500)
            makeBOOM();
    }
    public void makeBOOM(){
        Polygon bounds = new Polygon();
        for(int i = 0; i < 360; i += 5){
            double pX = (Math.pow(Math.random(), 0.1) * power * Math.cos(i * Math.PI / 180));
            double pY = (Math.pow(Math.random(), 0.1) * power * Math.sin(i * Math.PI / 180));
            bounds.addPoint((int)(x + pX), (int)(y + pY));
        }
        World.map.remove(new Area(bounds));
	dmgTank(new Area(bounds));
        exploded = true;
    }
    public void dmgTank(Area bounds){
	Area player1Intersection = World.player1.getBounds();
	player1Intersection.intersect(new Area(bounds));
	if(!player1Intersection.isEmpty())
	    World.player1.hit(power);
	Area player2Intersection = World.player2.getBounds();
	player2Intersection.intersect(new Area(bounds));
	if(!player2Intersection.isEmpty())
	    World.player2.hit(power);
    }
    public Bullets(){
        xmove = 0;
        ymove = 0;
    }
    public static void resetAll(){
        while(!bulletsFired.isEmpty())
            bulletsFired.remove(0);
    }
    static class Nuke extends Bullets implements Cloneable {
        public Nuke(){
            power = 60;
	    name = "Nuke";
        }
    }
    static class Bullet extends Bullets implements Cloneable {
        public Bullet(){
            power = 15;
	    name = "Standard Weapon";
        }
    }
    static class ClusterBomb extends Bullets implements Cloneable {
        public ClusterBomb(){
            power = 12;
	    name = "Cluster Bomb";
        }
        private static class ClusterFragment extends HoningBomb {
            public ClusterFragment(){
                power = 2;
		aimPower = 0.01;
		defaultTarget = true;
            }
	    @Override public void shoot(double angle, double power, double x, double y){
		ymove += power * Math.sin(angle) / 1.3;
		xmove += power * Math.cos(angle) / 1.3;
		this.x = x;
		this.y = y;
		bulletsFired.add(this);
	    }
        }
        @Override public void makeBOOM(){
            super.makeBOOM();
            for(int angleDegrees = 0; angleDegrees < 360; angleDegrees += 20){
                new ClusterFragment().shoot(((double) angleDegrees) * Math.PI / 180.0, 0.5, x, y);
            }
        }
	@Override public void move(){
	    super.move();
	    if(ymove > 1.2)
		makeBOOM();
	}
    }
    static class Miner extends Bullets implements Cloneable {
        int boomsLeft = 15;
        public Miner(){
            power = 2;
	    name = "Miner";
        }
        @Override public void makeBOOM(){
            Polygon bounds = new Polygon();
            for(int i = 0; i < 360; i += 5){
                double pX = (Math.pow(Math.random(), 0.1) * 15 * power * Math.cos(i * Math.PI / 180));
                double pY = (Math.pow(Math.random(), 0.1) * 15 * power * Math.sin(i * Math.PI / 180));
                bounds.addPoint((int)(x + pX), (int)(y + pY));
            }
            World.map.remove(new Area(bounds));
	    dmgTank(new Area(bounds));
            if(--boomsLeft <= 0){
                exploded = true;
            }
        }
    }
    static class HoningBomb extends Bullets implements Cloneable {
        boolean targeting = false;
	boolean defaultTarget = false;
	double threshold = 1;
	double aimPower = 0.1;
        public HoningBomb(){
	    power = 23;
	    name = "Honing Bomb";
	}
        @Override public void move(){
            if(ymove < threshold && !targeting && !defaultTarget){
                x += xmove / 2;
                y += ymove / 2;
                ymove += 0.2 / 2;
            }
            else {
                targeting = true;
                Tank target = World.window.tanks.get((World.turn + 1) % 2);
		double xAngle = Math.acos((target.x - x) / Math.sqrt(Math.pow((x - target.x), 2) + Math.pow((y - target.y), 2)));
		double yAngle = Math.asin((target.y - y) / Math.sqrt(Math.pow((x - target.x), 2) + Math.pow((y - target.y), 2)));
		xmove += (Math.cos(xAngle) * aimPower);
		xmove /= 1 + aimPower;
		ymove += (Math.sin(yAngle) * aimPower);
		ymove /= 1 + aimPower;
                x += xmove;
                y += ymove;
            }
            checkCollisions();
        }
    }
    static class DeathShaft extends Bullets implements Cloneable {
	public DeathShaft(){
	    power = 20;
	    name = "Death Shaft";
	}
	@Override public void makeBOOM(){
	    Polygon bounds = new Polygon();
	    for(int i = 0; i < 360; i += 5){
		double pX = (Math.pow(Math.random(), 0.02) * power * 3 * Math.cos(i * Math.PI / 180));
		double pY = (Math.pow(Math.random(), 0.02) * power * 15 * Math.sin(i * Math.PI / 180));
		bounds.addPoint((int)(x + pX), (int)(y + pY));
	    }
	    World.map.remove(new Area(bounds));
	    dmgTank(new Area(bounds));
	    exploded = true;
	}
    }
    static class DeathRain extends Bullets implements Cloneable {
	boolean rainBegan = false;
	int rainsLeft = 60;
	static class RainDrop extends Bullets {
	    public RainDrop(){
		power = 5;
	    }
	}
	public DeathRain(){
	    power = 30;
	    name = "Rain of Death";
	}
	@Override public void move(){
	    if(!rainBegan)
		super.move();
	    if(ymove > 1.5)
		makeRAIN();
	};
	@Override public void checkCollisions(){
	    Polygon bounds = new Polygon();
	    for(int i = 0; i < 360; i += 10){
		double pX = (2.5 * Math.cos(i * Math.PI / 180));
		double pY = (2.5 * Math.sin(i * Math.PI / 180));
		bounds.addPoint((int)(x + pX), (int)(y + pY));
	    }
	    Area minusGround = new Area(bounds);
	    minusGround.intersect(World.map.map);
	    Area explosion = new Area(bounds);
	    explosion.intersect(World.player1.getBounds());
	    if(!explosion.isEmpty())
		makeBOOM();
	    explosion = new Area(bounds);
	    explosion.intersect(World.player2.getBounds());
	    if(!explosion.isEmpty() && !exploded)
		makeBOOM();
	    if(!minusGround.isEmpty() && !exploded)
		makeBOOM();
	    if(x < 0 || x > Map.size || y > 500)
		makeBOOM();
	}
	public void makeRAIN(){
	    rainBegan = true;
	    int offset = (int)(80.0 * (Math.random() - 0.5));
	    if(rainsLeft % 4 == 0)
		new RainDrop().shoot(90, 0, x + offset, y);
	    if(--rainsLeft < 0){
		exploded = true;
	    }
	}
    }
    static class BottledMountain extends Bullets implements Cloneable {
	public BottledMountain(){
	    power = 60;
	    name = "Bottled Mountain";
	}
	@Override public void makeBOOM(){
        Polygon bounds = new Polygon();
        for(int i = 0; i < 360; i += 5){
            double pX = (Math.pow(Math.random(), 0.1) * power * Math.cos(i * Math.PI / 180));
            double pY = (Math.pow(Math.random(), 0.1) * power * Math.sin(i * Math.PI / 180));
            bounds.addPoint((int)(x + pX), (int)(y + pY));
        }
        World.map.add(new Area(bounds));
        exploded = true;
	}
    }
    static class Sniper extends Bullets implements Cloneable {
	public Sniper(){
	    power = 4;
	    name = "Sniper";
	}
	@Override public void shoot(double angle, double power, double x, double y){
	    super.shoot(angle, 12, x, y);
	}
	@Override public void move(){
	    x += xmove / 2;
	    y += ymove / 2;
	    checkCollisions();
	}
	@Override public void dmgTank(Area bounds){
	    power = 40;
	    super.dmgTank(bounds);
	    power = 4;
	}
    }
    static class FlyingTurret extends Bullets implements Cloneable {
	int ticksUntilNextShot = 10;
	int reloadTime = 10;
	int shotsLeft = 30;
	static class Shot extends Bullets {
	    public Shot(){
		power = 2;
	    }
	    @Override public void move(){
		x += xmove / 2;
		y += ymove / 2;
		checkCollisions();
	    }
	}
	public FlyingTurret(){
	    power = 10;
	    name = "Flying Turret";
	}
	@Override public void move(){
	    super.move();
	    if(--ticksUntilNextShot == 0 && shotsLeft > 0){
		ticksUntilNextShot = reloadTime;
		shotsLeft--;
		new Shot().shoot(Math.atan2((World.window.tanks.get((World.turn + 1) % 2).y - y), (World.window.tanks.get((World.turn + 1) % 2).x - x)), 5, x, y);
	    }
	}
    }
    static class Teleport extends Bullets implements Cloneable {
	public Teleport(){
	    power = 20;
	    name = "Teleporter";
	}
	@Override public void makeBOOM(){
	    Polygon bounds = new Polygon();
	    for(int i = 0; i < 360; i += 5){
		double pX = (5 * Math.cos(i * Math.PI / 180));
		double pY = (5 * Math.sin(i * Math.PI / 180));
		bounds.addPoint((int)(x + pX), (int)(y + pY));
	    }
	    dmgTank(new Area(bounds));
	    World.window.tanks.get(World.turn).x = (int) x;
	    exploded = true;
	}
    }
    static class SummonAirSupport extends Bullets implements Cloneable {
	public SummonAirSupport(){
	    name = "Summon Air support";
	}
    }
}
class Window extends JPanel implements KeyListener {
    JFrame frame = new JFrame("Tank Warfare");
    ArrayList<Tank> tanks = new ArrayList();
    Map map;
    boolean left, right;
    boolean aimLeft, aimRight;
    boolean powerUp, powerDown;
    int width = 1000;
    int height = 500;
    int posX, posY;
    int turn = 0;
    @Override public void paintComponent(Graphics G) throws ConcurrentModificationException {
        super.paintComponent(G);
        Graphics2D g = (Graphics2D) G;
        g.translate(0, 50);
        map.draw(g);
        for(Tank t : tanks)
            t.draw(g);
        for(Bullets b : Bullets.bulletsFired)//Causes problems due to some multithreading THAT I DIDN'T CODE! Must fix
            b.draw(g);
        g.setColor(new Color(0xEEEEFF));
        g.fillRect(0, -50, width, 50);
        g.setColor(Color.red);
        g.fillRect(930, -50, 60, 40);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Verdana", 1, 20));
        g.drawString("Tank Warfare ", 10, -15);
        g.setColor(new Color(0));             
        g.setFont(new Font("Verdana", 0, 20));      
        g.fillRect(0, -2, 1000, 2);
        g.setColor(Color.GRAY);
        g.fillRect(0, height, width, 75);
        g.setColor(Color.BLACK);
        g.drawString("Player 1 health", (int)(width * 0.1), height + 25);
        g.drawString("Player 2 health", (int)(width * 0.7), height + 25);
	g.setColor(Color.RED);
	g.fillRect((int)(width * 0.1), height + 30, 150, 20);
	g.fillRect((int)(width * 0.7), height + 30, 150, 20);
	g.setColor(Color.GREEN);
	g.fillRect((int)(width * 0.1), height + 30, (int)(tanks.get(0).life * 1.5), 20);
	g.fillRect((int)(width * 0.7), height + 30, (int)(tanks.get(1).life * 1.5), 20);
	g.setColor(Color.BLACK);
	int textWidth = g.getFontMetrics().stringWidth("Selected weapon: " + tanks.get(turn).bullets.get(tanks.get(turn).selectedWeapon).name);
	g.drawString("Selected weapon: " + tanks.get(turn).bullets.get(tanks.get(turn).selectedWeapon).name, width / 2 - textWidth / 2, height + 35);
    }
    public Window(){
        super();
        setPreferredSize(new Dimension(width, height + 125));
        setBackground(Color.white);
    }
    public void loop(){
        while(true){
	    if(World.noTurn && Bullets.bulletsFired.isEmpty()){
		World.nextTurn();
		for(Tank t : tanks)
		    t.driveLeft = 200;
	    }
	    else if(!World.noTurn){
		if(left)
		    tanks.get(turn).drive(-1);
		if(right)
		    tanks.get(turn).drive(1);
		if(aimLeft)
		    tanks.get(turn).barrelAim -= 1;
		if(aimRight)
		    tanks.get(turn).barrelAim += 1;
		if(powerUp)
		    tanks.get(turn).power += 0.05;
		if(powerDown)
		    tanks.get(turn).power -= 0.05;
	    }
            tanks.get(turn).updateLocation();
            tanks.get((1 + turn) % 2).updateLocation();
            for(int i = 0; i < Bullets.bulletsFired.size(); i++){
                Bullets b = Bullets.bulletsFired.get(i);
                b.move();
                if(b.exploded){
                    Bullets.bulletsFired.remove(b);
                    i--;
                }
            }
            repaint();
            try {
                Thread.sleep(4);
            }
            catch(Exception e){}
        }
    }
    public void addTank(Tank t){
        tanks.add(t);
    }
    public void setMap(Map m){
        map = m;
    }
    public void setTurn(int t){
        turn = t;
    }
    public void initialize(){
        frame.add(this);
        frame.setDefaultCloseOperation(3);
        frame.setUndecorated(true);
        frame.pack();
        frame.setVisible(true);
        frame.setFocusable(true);
        frame.addKeyListener(this);
        frame.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent me){
                if(new Rectangle(930, 0, 60, 40).contains(me.getX(), me.getY()))
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
            @Override public void mousePressed(MouseEvent e){
                posX = e.getX();
                posY = e.getY();
            }
        });
        frame.addMouseMotionListener(new MouseAdapter(){
            @Override public void mouseDragged(MouseEvent evt){
                Rectangle rectangle = getBounds();
                if(!new Rectangle(0, 0, 1000, 50).contains(posX, posY))
                    return;
                frame.setBounds(evt.getXOnScreen() - posX, evt.getYOnScreen() - posY, rectangle.width, rectangle.height);
            }
        });
        repaint();
        loop();
    }
    public void close(){
	frame.dispose();
    }
    @Override public void keyTyped(KeyEvent e){}
    @Override public void keyPressed(KeyEvent e){
        if(e.getKeyCode() == KeyEvent.VK_LEFT)
            left = true;
        if(e.getKeyCode() == KeyEvent.VK_RIGHT)
            right = true;
        if(e.getKeyChar() == 'a')
            aimLeft = true;
        if(e.getKeyChar() == 'd')
            aimRight = true;
        if(e.getKeyChar() == 'q')
            tanks.get(turn).scroll(-1);
        if(e.getKeyChar() == 'e')
            tanks.get(turn).scroll(1);
        if(e.getKeyChar() == ' ' && !World.noTurn)
            tanks.get(turn).fire();
        if(e.getKeyChar() == ']')
            powerUp = true;
        if(e.getKeyChar() == '[')
            powerDown = true;
    }
    @Override public void keyReleased(KeyEvent e){
        if(e.getKeyCode() == KeyEvent.VK_LEFT)
            left = false;
        if(e.getKeyCode() == KeyEvent.VK_RIGHT)
            right = false;
        if(e.getKeyChar() == 'a')
            aimLeft = false;
        if(e.getKeyChar() == 'd')
            aimRight = false;
        if(e.getKeyChar() == ']')
            powerUp = false;
        if(e.getKeyChar() == '[')
            powerDown = false;
    }
}
class TankBarrel extends Polygon {
    public double angle = Math.PI / 4;
    public double toRadians = Math.PI / 180;
    int width = 8;
    int height = 40;
    int x, y;
    public TankBarrel(int x, int y){
        super();
        this.x = x;
        this.y = y;
    }
    public void move(int x, int y){
        this.x = x;
        this.y = y;
        updatePoints();
    }
    public void setAngle(double angle){
        this.angle = angle;
        updatePoints();
    }
    public void changeAngle(double degrees){
        angle += degrees;
        updatePoints();
    }
    public void updatePoints(){
        reset();
        double distance1 = width / 2;
        double distance2 = height - width;
        int x1 = x + (int) + (distance1 * Math.sqrt(2) * Math.cos(angle - Math.PI * 3 / 4));
        int y1 = y + (int) + (distance1 * Math.sqrt(2) * Math.sin(angle - Math.PI * 3 / 4));
        int x2 = x + (int) + (distance1 * Math.sqrt(2) * Math.cos(angle + Math.PI * 3 / 4));
        int y2 = y + (int) + (distance1 * Math.sqrt(2) * Math.sin(angle + Math.PI * 3 / 4));
        int newx = x + (int)(distance2 * Math.cos(angle));
        int newy = y + (int)(distance2 * Math.sin(angle));
        int x3 = newx + (int) + (distance1 * Math.sqrt(2) * Math.cos(angle - Math.PI * 1 / 4));
        int y3 = newy + (int) + (distance1 * Math.sqrt(2) * Math.sin(angle - Math.PI * 1 / 4));
        int x4 = newx + (int) + (distance1 * Math.sqrt(2) * Math.cos(angle + Math.PI * 1 / 4));
        int y4 = newy + (int) + (distance1 * Math.sqrt(2) * Math.sin(angle + Math.PI * 1 / 4));
        addPoint(x1, y1);
        addPoint(x2, y2);
        addPoint(x4, y4);
        addPoint(x3, y3);
    }
    public double[] getTip(){
        double distance = height - width + 20;
        double[] coords = {x + distance * Math.cos(angle), y + distance * Math.sin(angle)};
        return coords; 
    }
    public void draw(Graphics2D G){
        G.setColor(Color.RED);
        G.fillPolygon(this);
    }
    public Area getArea(){
        return new Area(this);
    }
}
class TankBody extends Polygon {
    public double toRadian = Math.PI / 180;
    double slope = 0;
    int width = 50;
    int height = 20;
    int x, y;
    public TankBody(int x, int y){
        this.x = x;
        this.y = y;
        updatePoints();
    }
    public void setAngle(double slope){
        this.slope = slope;
        updatePoints();
    }
    public void move(int x, int y){
        this.x = x;
        this.y = y;
        updatePoints();
    }
    public final void updatePoints(){
        this.reset();
        int distance = width / 2;
        double a = slope * slope + 1;
        double c = -(distance * distance);
        double x1 = x - Math.sqrt(-4 * a * c)/(a * 2);
        double y1 = y - Math.sqrt(-4 * a * c)/(a * 2) * slope;
        double x2 = x + Math.sqrt(-4 * a * c)/(a * 2);
        double y2 = y + Math.sqrt(-4 * a * c)/(a * 2) * slope;
        double newSlope = -1 / slope;
        double newA = newSlope * newSlope + 1;
        double x3, y3, x4, y4;
        if(Double.isInfinite(newSlope)){
            x3 = x2;
            x4 = x1;
            y4 = y1 - height;
            y3 = y2 - height;
        }
        else {
            double newC = -(height * height);
            x3 = x2 - Math.signum(newSlope) * Math.sqrt(-4 * newA * newC)/(newA * 2);
            x4 = x1 - Math.signum(newSlope) * Math.sqrt(-4 * newA * newC)/(newA * 2);
            y4 = y1 - Math.signum(newSlope) * Math.sqrt(-4 * newA * newC)/(newA * 2) * newSlope;
            y3 = y2 - Math.signum(newSlope) * Math.sqrt(-4 * newA * newC)/(newA * 2) * newSlope;
        }
        addPoint((int) x1, (int) y1);
        addPoint((int) x2, (int) y2);
        addPoint((int) x3, (int) y3);
        addPoint((int) x4, (int) y4);
    }
    public void draw(Graphics2D G){
        G.setColor(Color.blue);
        G.fillPolygon(this);
    }
    public Area getArea(){
        return new Area(this);
    }
}
class World {
    static Map map;
    static Window window;
    static Tank player1, player2;
    public static int turn = 0;
    public static boolean noTurn = false;
    public static double windFactor = 0.02 * (Math.random() - 0.5);
    public World(){
        map = new Map();
        window = new Window();
        player1 = new Tank(250);
        player2 = new Tank(750);
        turn = 0;
        window.setMap(map);
        window.addTank(player1);
        window.addTank(player2);
        window.initialize();
    }
    public static void reset(){
	      window.close();
        map = new Map();
        window = new Window();
        player1 = new Tank(250);
        player2 = new Tank(750);
        turn = 0;
        window.setMap(map);
        window.addTank(player1);
        window.addTank(player2);
        window.initialize();
	      noTurn = false;
    }
    public static void noTurn(){
	    noTurn = true;
    }
    public static void nextTurn(){
        turn++;
        turn %= 2;
	      noTurn = false;
        window.setTurn(turn);
    }
}
