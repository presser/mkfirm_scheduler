options(digits = 4, width = 100) #set precision for float values and and chars by line

res <- read.csv(file = "../results.csv",na.strings = c("NA",""," "),header=TRUE,sep = ";");

e1 <- subset(res, stream == "starve1");
s1 <- subset(res, stream == "stream1");
s2 <- subset(res, stream == "stream2");
s <- data.frame(x=s1$streams.0.dynamicFaultChances, y=(s1$dynamic_fault_count+s2$dynamic_fault_count)/2)
plot(x=e1$streams.0.dynamicFaultChances,
     y=e1$dynamic_fault_count,
     xlab = "dynamicFaultChances", ylab = "dynamicFaults",
     pch=1, xlim=c(2,10), ylim=c(0,15));
points(x=s$x,y=s$y,pch=3);
legend(x=2, y=13,legend=c("starve1", "avg streams"), pch=c(1, 3));

