load data_2Mill2D.csv

x_data = data_2Mill2D(:,2);
y_data = data_2Mill2D(:,3);

figure
plot(x_data,y_data,'.k');
title('Original data points');

%% SPARK RESULTS
weight1 = 0.451790;
mu1 = [3.619336283222613 1.647695532534273];
sigma1 = [4.7954770965629985 -1.1787613286529137; -1.1787613286529137 1.638589448003014];

weight2 = 0.104867;
mu2 = [8.88536436818748 9.917514484797353];
sigma2 = [4.201726056435331 -0.05243951195588813; -0.05243951195588813 1.5415252549863603]; 

weight3 = 0.232660;
mu3 = [2.50359466932094 2.1533311312002072];
sigma3 = [5.391270032037676 -1.670421202836598; -1.670421202836598 9.64296135232161];

weight4 = 0.210683;
mu4 = [4.042176544507779 3.362641894673466];
sigma4 = [9.666971979463948 0.24360795314877873; 0.24360795314877873 2.8448385796961686];


% GAUSSIAN 1
x1 = -10:.2:20; y1 = -10:.2:15;
[X1,Y1] = meshgrid(x1,y1);
F = mvnpdf([X1(:) Y1(:)],mu1,sigma1);
F1 = reshape(F,length(y1),length(x1));

% GAUSSIAN 2
x2 = -10:.2:20; y2 = -10:.2:15;
[X2,Y2] = meshgrid(x2,y2);
F = mvnpdf([X2(:) Y2(:)],mu2,sigma2);
F2 = reshape(F,length(y2),length(x2));

% GAUSSIAN 3
x3 = -10:.2:20; y3 = -10:.2:15;
[X3,Y3] = meshgrid(x3,y3);
F = mvnpdf([X3(:) Y3(:)],mu3,sigma3);
F3 = reshape(F,length(y3),length(x3));

x4 = -10:.2:20; y4 = -10:.2:15;
[X4,Y4] = meshgrid(x4,y4);
F = mvnpdf([X4(:) Y4(:)],mu4,sigma4);
F4 = reshape(F,length(y4),length(x4));

% figure %plot 2D gaussian 1
% surf(x1,x2,F);
% caxis([min(F(:))-.5*range(F(:)),max(F(:))]);
% axis([-4 4 -4 4 0 .4])
% xlabel('x1'); ylabel('x2'); zlabel('Probability Density');

% figure %plot 2D gaussian 2
% surf(x1b,x2b,F2);
% caxis([min(F2(:))-.5*range(F2(:)),max(F2(:))]);
% axis([-4 4 -4 4 0 .4])
% xlabel('x1'); ylabel('x2'); zlabel('Probability Density');


% CONTOUR PLOT
figure %first gaussian
plot(x_data,y_data,'.k');
title('Spark results');
hold on;
mvncdf([0 0],[1 1],mu1,sigma1);
v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c1a,h1a]=contour(x1,y1,F1,v1);
set(h1a,'color','b');
xlabel('x'); ylabel('y');
hold on;
mvncdf([0 0],[1 1],mu2,sigma2);
v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c2a,h2a] = contour(x2,y2,F2,v2);
set(h2a,'color','r');
%line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');
hold on;
mvncdf([0 0],[1 1],mu3,sigma3);
v3=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c3a,h3a] = contour(x3,y3,F3,v3);
set(h3a,'color','g');
hold on;
mvncdf([0 0],[1 1],mu4,sigma4);
v4=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c4a,h4a] = contour(x4,y4,F4,v4);
set(h4a,'color','m');

% %% FLINK INITAL CLUSTERS
% 
% mu1 = [2.6615935738323055 4.931058041910233 ];
% sigma1 = [1.8725467818108206 -0.4880598743572286; -0.4880598743572286 2.9159400268450906 ];
% mu2 = [8.474197699284689 7.896826343168253 ];
% sigma2 = [3.6453263770201976 1.4525378092041576; 1.4525378092041576 9.138746901589704 ];
% mu3 = [-0.1618211040344874 2.8798863770036376 ];
% sigma3 = [1.7015112877305056 -0.4593004077226671; -0.4593004077226671 2.0454036832757403 ];
% mu4 = [4.189291992345398 1.0554697638552077 ];
% sigma4 = [2.961530274958424 0.11805974553352713; 0.11805974553352713 2.1778617071500594 ];
% 
% % mu1 = [-1.8882790061372836 -0.6880304391241152 ];
% % sigma1 = [2.3879830542069747 1.0495393398654975; 1.0495393398654975 0.7136672309307922 ];
% % mu2 = [-0.10510291974793552 0.029291091582435057 ];
% % sigma2 = [4.662623197220264 -1.9121816459448413; -1.9121816459448413 0.9703466511178434 ];
% % mu3 = [1.285944074308319 0.5055638292534829 ];
% % sigma3 = [2.387585418844933 0.9224992563910634; 0.9224992563910634 0.4961299991558006 ];
% % mu4 = [3.4107461612067285 0.5466991899569401 ];
% % sigma4 = [2.0507785418287674 0.6829980462676679; 0.6829980462676679 1.7498396827061746 ];
% 
% 
% x1 = -10:.2:20; y1 = -10:.2:15;
% [X1,Y1] = meshgrid(x1,y1);
% F = mvnpdf([X1(:) Y1(:)],mu1,sigma1);
% F1 = reshape(F,length(y1),length(x1));
% 
% % GAUSSIAN 2
% x2 = -10:.2:20; y2 = -10:.2:15;
% [X2,Y2] = meshgrid(x2,y2);
% F = mvnpdf([X2(:) Y2(:)],mu2,sigma2);
% F2 = reshape(F,length(y2),length(x2));
% 
% % GAUSSIAN 3
% x3 = -10:.2:20; y3 = -10:.2:15;
% [X3,Y3] = meshgrid(x3,y3);
% F = mvnpdf([X3(:) Y3(:)],mu3,sigma3);
% F3 = reshape(F,length(y3),length(x3));
% 
% x4 = -10:.2:20; y4 = -10:.2:15;
% [X4,Y4] = meshgrid(x4,y4);
% F = mvnpdf([X4(:) Y4(:)],mu4,sigma4);
% F4 = reshape(F,length(y4),length(x4));
% 
% 
% % CONTOUR PLOT
% figure %first gaussian
% %plot(x_data,y_data,'.k');
% %hold on;
% mvncdf([0 0],[1 1],mu1,sigma1);
% v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c1a,h1a]=contour(x1,y1,F1,v1);
% set(h1a,'color','b');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2a,h2a] = contour(x2,y2,F2,v2);
% set(h2a,'color','r');
% hold on;
% mvncdf([0 0],[1 1],mu3,sigma3);
% v3=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c3a,h3a] = contour(x3,y3,F3,v3);
% set(h3a,'color','g');
% hold on;
% mvncdf([0 0],[1 1],mu4,sigma4);
% v4=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c4a,h4a] = contour(x4,y4,F4,v4);
% set(h4a,'color','c');


%% FLINK FINAL CLUSTERS

 
%Cluster 1 PI: 0.1186480102397498
mu1 = [8.33983778701829 9.681765291999104 ];
sigma1 = [6.925368180182883 0.6372023839763491; 0.6372023839763491 2.0219577293300333 ];
%Cluster 2 PI: 0.30478049747763636
mu2 = [5.376078000260129 1.8095533805383983 ];
sigma2 = [3.8608854226116742 0.48242310172607444; 0.48242310172607444 2.990664427503785 ];
%Cluster 3 PI: 0.3572147060373993
mu3 = [1.7546115213241593 2.6788047376622033 ];
sigma3 = [4.689708434186356 -1.3707287586061632; -1.3707287586061632 3.075849452015572 ];
%Cluster 4 PI: 0.21935678624520488 
mu4 = [3.4020524859586976 1.535079654982893 ];
sigma4 = [2.8401185503408293 0.34243568061592694; 0.34243568061592694 5.911943237756805 ];


x1 = -10:.2:20; y1 = -10:.2:15;
[X1,Y1] = meshgrid(x1,y1);
F = mvnpdf([X1(:) Y1(:)],mu1,sigma1);
F1 = reshape(F,length(y1),length(x1));

% GAUSSIAN 2
x2 = -10:.2:20; y2 = -10:.2:15;
[X2,Y2] = meshgrid(x2,y2);
F = mvnpdf([X2(:) Y2(:)],mu2,sigma2);
F2 = reshape(F,length(y2),length(x2));

% GAUSSIAN 3
x3 = -10:.2:20; y3 = -10:.2:15;
[X3,Y3] = meshgrid(x3,y3);
F = mvnpdf([X3(:) Y3(:)],mu3,sigma3);
F3 = reshape(F,length(y3),length(x3));

x4 = -10:.2:20; y4 = -10:.2:15;
[X4,Y4] = meshgrid(x4,y4);
F = mvnpdf([X4(:) Y4(:)],mu4,sigma4);
F4 = reshape(F,length(y4),length(x4));


% CONTOUR PLOT
figure %first gaussian
plot(x_data,y_data,'.k');
title('Flink results');
hold on;
mvncdf([0 0],[1 1],mu1,sigma1);
v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c1a,h1a]=contour(x1,y1,F1,v1);
set(h1a,'color','b');
xlabel('x'); ylabel('y');
hold on;
mvncdf([0 0],[1 1],mu2,sigma2);
v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c2a,h2a] = contour(x2,y2,F2,v2);
set(h2a,'color','r');
hold on;
mvncdf([0 0],[1 1],mu3,sigma3);
v3=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c3a,h3a] = contour(x3,y3,F3,v3);
set(h3a,'color','g');
hold on;
mvncdf([0 0],[1 1],mu4,sigma4);
v4=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c4a,h4a] = contour(x4,y4,F4,v4);
set(h4a,'color','m');

%% PLOT FINAL CLUSTERS

load GMM_results_100000_4.csv
c_data = GMM_results_100000_4(:,1);
x_data = GMM_results_100000_4(:,2);
y_data = GMM_results_100000_4(:,3);
figure
gscatter(x_data,y_data,c_data,'brgm','....');

